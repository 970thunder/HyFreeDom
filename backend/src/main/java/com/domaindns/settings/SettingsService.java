package com.domaindns.settings;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {
    private final SettingsMapper mapper;

    public SettingsService(SettingsMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, String> getAll() {
        List<SettingsRow> rows = mapper.findAll();
        Map<String, String> m = new HashMap<>();
        for (SettingsRow r : rows)
            m.put(r.getK(), r.getV());
        return m;
    }

    public String get(String key, String defaultValue) {
        String val = mapper.getValue(key);
        return val != null ? val : defaultValue;
    }

    public void update(Map<String, String> body) {
        if (body == null)
            return;
        for (Map.Entry<String, String> e : body.entrySet()) {
            SettingsRow r = new SettingsRow();
            r.setK(e.getKey());
            r.setV(e.getValue());
            mapper.upsert(r);
        }
    }
}
