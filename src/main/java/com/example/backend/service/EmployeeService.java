package com.example.backend.service;

import com.example.backend.model.Employee;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EmployeeService {
    private final Map<Long, Employee> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public List<Employee> list() { return new ArrayList<>(store.values()); }

    public Employee create(Employee e) {
        long id = seq.getAndIncrement();
        e.setId(id);
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        store.put(id, e);
        return e;
    }

    public Employee get(Long id) { return store.get(id); }

    public Employee update(Long id, Employee e) {
        Employee existing = store.get(id);
        if (existing == null) return null;
        e.setId(id);
        store.put(id, e);
        return e;
    }

    public boolean delete(Long id) { return store.remove(id) != null; }
}

