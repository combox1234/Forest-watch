package com.ds.sensor.central.service;

import com.ds.sensor.central.model.UserEntity;
import com.ds.sensor.central.repository.UserRepository;
import com.ds.sensor.shared.model.AppRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class ForestWatchDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ForestWatchDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${forestwatch.security.demo-password:ForestWatch@2026}")
    private String defaultDemoPassword;

    @Autowired
    public ForestWatchDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing ForestWatch RBAC Demo Accounts...");

        record DemoSeed(String username, String displayName, AppRole role) {}

        List<DemoSeed> seeds = List.of(
                new DemoSeed("priya.admin", "Priya Sharma", AppRole.ADMINISTRATOR),
                new DemoSeed("arjun.operator", "Arjun Patil", AppRole.FOREST_OPERATOR),
                new DemoSeed("ananya.viewer", "Ananya Deshmukh", AppRole.VIEWER),
                new DemoSeed("rahul.operator", "Rahul Verma", AppRole.FOREST_OPERATOR),
                new DemoSeed("kavya.viewer", "Kavya Iyer", AppRole.VIEWER)
        );

        String encodedPassword = passwordEncoder.encode(defaultDemoPassword);

        for (DemoSeed s : seeds) {
            if (!userRepository.existsByUsername(s.username())) {
                UserEntity user = new UserEntity(s.username(), encodedPassword, s.displayName(), s.role(), true);
                userRepository.save(user);
                log.info("Seeded ForestWatch demo user: username='{}', name='{}', role={}", s.username(), s.displayName(), s.role());
            }
        }
    }
}
