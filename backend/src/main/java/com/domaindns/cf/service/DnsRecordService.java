package com.domaindns.cf.service;

import com.domaindns.cf.mapper.CfAccountMapper;
import com.domaindns.cf.mapper.DnsRecordMapper;
import com.domaindns.cf.mapper.ZoneMapper;
import com.domaindns.cf.model.CfAccount;
import com.domaindns.cf.model.DnsRecord;
import com.domaindns.cf.model.Zone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class DnsRecordService {
    private final DnsRecordMapper recordMapper;
    private final ZoneMapper zoneMapper;
    private final CfAccountMapper accMapper;
    private final CfClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DnsRecordService(DnsRecordMapper recordMapper, ZoneMapper zoneMapper, CfAccountMapper accMapper,
            CfClient client) {
        this.recordMapper = recordMapper;
        this.zoneMapper = zoneMapper;
        this.accMapper = accMapper;
        this.client = client;
    }

    public void syncZoneRecords(Long zoneDbId) {
        Zone z = zoneMapper.list(null, null, null).stream().filter(x -> x.getId().equals(zoneDbId)).findFirst()
                .orElse(null);
        if (z == null)
            throw new IllegalArgumentException("zone 不存在");
        CfAccount acc = accMapper.findById(z.getCfAccountId());
        if (acc == null || acc.getEnabled() == null || acc.getEnabled() == 0)
            throw new IllegalArgumentException("对应账户不可用");

        // 收集Cloudflare中的所有记录ID
        Set<String> cfRecordIds = new HashSet<>();
        int page = 1, per = 100;
        boolean hasMore = true;

        while (hasMore) {
            String json = client.listDnsRecords(acc, z.getZoneId(), page, per).block();
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode result = root.get("result");
                int pageSaved = 0;
                if (result != null && result.isArray()) {
                    for (JsonNode n : result) {
                        String cfRecordId = n.get("id").asText();
                        cfRecordIds.add(cfRecordId);

                        DnsRecord r = new DnsRecord();
                        r.setZoneId(z.getId());
                        r.setCfRecordId(cfRecordId);
                        r.setName(n.get("name").asText());
                        r.setType(n.get("type").asText());
                        r.setContent(n.path("content").asText(""));
                        r.setTtl(n.path("ttl").asInt(120));
                        r.setProxied(n.path("proxied").asBoolean(false) ? 1 : 0);
                        recordMapper.upsert(r);
                        pageSaved++;
                    }
                }
                int totalPages = root.path("result_info").path("total_pages").asInt(1);
                hasMore = page < totalPages && pageSaved > 0;
                page++;
            } catch (Exception e) {
                throw new IllegalStateException("解析 Cloudflare 记录响应失败");
            }
        }

        // 删除数据库中不在Cloudflare中的记录
        List<DnsRecord> dbRecords = recordMapper.listByZone(zoneDbId, null, null);
        for (DnsRecord dbRecord : dbRecords) {
            if (!cfRecordIds.contains(dbRecord.getCfRecordId())) {
                System.out.println("删除不存在的记录: " + dbRecord.getCfRecordId() + " (" + dbRecord.getName() + ")");
                recordMapper.deleteByZoneAndCfRecordId(zoneDbId, dbRecord.getCfRecordId());
            }
        }
    }

    public List<DnsRecord> list(Long zoneDbId, String type, String name) {
        return recordMapper.listByZone(zoneDbId, type, name);
    }

    public List<com.domaindns.cf.dto.DnsRecordDtos.DnsRecordWithUser> listWithUser(Long zoneDbId, String type,
            String name) {
        return recordMapper.listByZoneWithUser(zoneDbId, type, name);
    }

    public void create(Long zoneDbId, String bodyJson) throws Exception {
        Zone z = zoneById(zoneDbId);
        CfAccount acc = accById(z.getCfAccountId());
        String resp;
        try {
            resp = client.createDnsRecord(acc, z.getZoneId(), bodyJson).block();
        } catch (WebClientResponseException wex) {
            String body = wex.getResponseBodyAsString();
            throw new IllegalStateException(body != null && !body.isEmpty() ? body
                    : (wex.getStatusCode() + " " + wex.getStatusText()));
        }
        JsonNode root = objectMapper.readTree(resp);
        if (!root.path("success").asBoolean(false))
            throw new IllegalStateException(root.path("errors").toString());
        JsonNode n = root.path("result");
        DnsRecord r = new DnsRecord();
        r.setZoneId(z.getId());
        r.setCfRecordId(n.get("id").asText());
        r.setName(n.get("name").asText());
        r.setType(n.get("type").asText());
        r.setContent(n.path("content").asText(""));
        r.setTtl(n.path("ttl").asInt(120));
        r.setProxied(n.path("proxied").asBoolean(false) ? 1 : 0);
        recordMapper.upsert(r);
    }

    public void update(Long zoneDbId, String recordId, String bodyJson) throws Exception {
        Zone z = zoneById(zoneDbId);
        CfAccount acc = accById(z.getCfAccountId());
        String resp;
        try {
            resp = client.updateDnsRecord(acc, z.getZoneId(), recordId, bodyJson).block();
        } catch (WebClientResponseException wex) {
            String body = wex.getResponseBodyAsString();
            // 如果记录不存在，尝试重新同步后再试一次
            if (body != null && body.contains("Record does not exist")) {
                System.out.println("记录不存在，尝试重新同步: " + recordId);
                syncZoneRecords(zoneDbId);
                // 重新尝试更新
                try {
                    resp = client.updateDnsRecord(acc, z.getZoneId(), recordId, bodyJson).block();
                } catch (WebClientResponseException retryEx) {
                    String retryBody = retryEx.getResponseBodyAsString();
                    throw new IllegalStateException(retryBody != null && !retryBody.isEmpty() ? retryBody
                            : (retryEx.getStatusCode() + " " + retryEx.getStatusText()));
                }
            } else {
                throw new IllegalStateException(body != null && !body.isEmpty() ? body
                        : (wex.getStatusCode() + " " + wex.getStatusText()));
            }
        }
        JsonNode root = objectMapper.readTree(resp);
        if (!root.path("success").asBoolean(false))
            throw new IllegalStateException(root.path("errors").toString());
        JsonNode n = root.path("result");
        DnsRecord r = new DnsRecord();
        r.setZoneId(z.getId());
        r.setCfRecordId(n.get("id").asText());
        r.setName(n.get("name").asText());
        r.setType(n.get("type").asText());
        r.setContent(n.path("content").asText(""));
        r.setTtl(n.path("ttl").asInt(120));
        r.setProxied(n.path("proxied").asBoolean(false) ? 1 : 0);
        recordMapper.upsert(r);
    }

    public void delete(Long zoneDbId, String recordId) throws Exception {
        Zone z = zoneById(zoneDbId);
        CfAccount acc = accById(z.getCfAccountId());
        String resp;
        try {
            resp = client.deleteDnsRecord(acc, z.getZoneId(), recordId).block();
        } catch (WebClientResponseException wex) {
            String body = wex.getResponseBodyAsString();
            // 如果记录不存在，尝试重新同步后再试一次
            if (body != null && body.contains("Record does not exist")) {
                System.out.println("记录不存在，尝试重新同步: " + recordId);
                syncZoneRecords(zoneDbId);
                // 重新尝试删除
                try {
                    resp = client.deleteDnsRecord(acc, z.getZoneId(), recordId).block();
                } catch (WebClientResponseException retryEx) {
                    String retryBody = retryEx.getResponseBodyAsString();
                    throw new IllegalStateException(retryBody != null && !retryBody.isEmpty() ? retryBody
                            : (retryEx.getStatusCode() + " " + retryEx.getStatusText()));
                }
            } else {
                throw new IllegalStateException(body != null && !body.isEmpty() ? body
                        : (wex.getStatusCode() + " " + wex.getStatusText()));
            }
        }
        JsonNode root = objectMapper.readTree(resp);
        if (!root.path("success").asBoolean(false))
            throw new IllegalStateException(root.path("errors").toString());
        // 本地删除交由后续同步清理，或可扩展 mapper 删除接口
    }

    private Zone zoneById(Long id) {
        return zoneMapper.list(null, null, null).stream().filter(x -> x.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("zone 不存在"));
    }

    private CfAccount accById(Long id) {
        CfAccount a = accMapper.findById(id);
        if (a == null)
            throw new IllegalArgumentException("账户不存在");
        return a;
    }

}