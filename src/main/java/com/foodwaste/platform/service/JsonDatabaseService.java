package com.foodwaste.platform.service;

import com.foodwaste.platform.model.DataState;
import com.foodwaste.platform.model.Donation;
import com.foodwaste.platform.model.RequestRecord;
import com.foodwaste.platform.model.UserEntity;
import com.foodwaste.platform.repository.ContentRepository;
import com.foodwaste.platform.repository.DonationRepository;
import com.foodwaste.platform.repository.RequestRepository;
import com.foodwaste.platform.repository.SettingsRepository;
import com.foodwaste.platform.repository.SignupOtpRepository;
import com.foodwaste.platform.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class JsonDatabaseService {

    private final UserRepository userRepository;
    private final SignupOtpRepository signupOtpRepository;
    private final DonationRepository donationRepository;
    private final RequestRepository requestRepository;
    private final ContentRepository contentRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordService passwordService;
    private final Object lock = new Object();

    public JsonDatabaseService(
        UserRepository userRepository,
        SignupOtpRepository signupOtpRepository,
        DonationRepository donationRepository,
        RequestRepository requestRepository,
        ContentRepository contentRepository,
        SettingsRepository settingsRepository,
        PasswordService passwordService
    ) {
        this.userRepository = userRepository;
        this.signupOtpRepository = signupOtpRepository;
        this.donationRepository = donationRepository;
        this.requestRepository = requestRepository;
        this.contentRepository = contentRepository;
        this.settingsRepository = settingsRepository;
        this.passwordService = passwordService;
    }

    @PostConstruct
    public void initialize() {
        synchronized (lock) {
            userRepository.ensureTable();
            signupOtpRepository.ensureTable();
            donationRepository.ensureTable();
            requestRepository.ensureTable();
            contentRepository.ensureTable();
            settingsRepository.ensureTable();

            DataState seed = seedState();
            userRepository.seedUsersIfEmpty(seed.users);
            donationRepository.seedIfEmpty(seed.donations);
            requestRepository.seedIfEmpty(seed.requests);
            contentRepository.seedIfEmpty(seed.content);
            settingsRepository.seedIfEmpty(seed.settings);
        }
    }

    public DataState readState() {
        synchronized (lock) {
            DataState state = new DataState();
            state.meta.put("source", "mysql");
            state.meta.put("version", 2);
            state.meta.put("generatedAt", Instant.now().toString());
            state.users = new ArrayList<>(userRepository.listUsers());
            state.donations = new ArrayList<>(donationRepository.listAll());
            state.requests = new ArrayList<>(requestRepository.listAll());
            state.content = new LinkedHashMap<>(contentRepository.listAll());
            state.settings = new LinkedHashMap<>(settingsRepository.listAll());
            ensureCollections(state);
            return state;
        }
    }

    public DataState mutate(Function<DataState, DataState> mutator) {
        synchronized (lock) {
            DataState current = readState();
            DataState updated = mutator.apply(current);
            ensureCollections(updated);
            replaceAll(updated);
            return updated;
        }
    }

    public void replaceAll(DataState state) {
        synchronized (lock) {
            ensureCollections(state);
            userRepository.replaceAll(state.users);
            donationRepository.replaceAll(state.donations);
            requestRepository.replaceAll(state.requests);
            contentRepository.replaceAll(state.content);
            settingsRepository.replaceAll(state.settings);
        }
    }

    private void ensureCollections(DataState state) {
        if (state.meta == null) {
            state.meta = new LinkedHashMap<>();
        }
        if (state.users == null) {
            state.users = new ArrayList<>();
        }
        if (state.donations == null) {
            state.donations = new ArrayList<>();
        }
        if (state.requests == null) {
            state.requests = new ArrayList<>();
        }
        if (state.reports == null) {
            state.reports = new ArrayList<>();
        }
        if (state.content == null) {
            state.content = new LinkedHashMap<>();
        }
        state.content.putIfAbsent("pages", new ArrayList<>());
        state.content.putIfAbsent("faqs", new ArrayList<>());
        state.content.putIfAbsent("announcements", new ArrayList<>());
        if (state.settings == null) {
            state.settings = new LinkedHashMap<>();
        }
        state.settings.putIfAbsent("general", new LinkedHashMap<>());
        state.settings.putIfAbsent("notifications", new LinkedHashMap<>());
        state.settings.putIfAbsent("donations", new LinkedHashMap<>());
        state.settings.putIfAbsent("security", new LinkedHashMap<>());
    }

    private DataState seedState() {
        Instant now = Instant.now();
        String donorId = UUID.randomUUID().toString();
        String recipientId = UUID.randomUUID().toString();
        String analystId = UUID.randomUUID().toString();
        String adminId = UUID.randomUUID().toString();

        UserEntity admin = seedUser(adminId, "admin@foodwaste.com", "Admin@123", "Administrator", "admin", "+1-800-FOOD-AID", "New York, NY", now);
        UserEntity donor = seedUser(donorId, "green@grocery.com", "Donor@123", "Green Grocery", "donor", "+1-234-567-8901", "New York, NY", now);
        UserEntity recipient = seedUser(recipientId, "contact@community.org", "Recipient@123", "Community Kitchen", "recipient", "+1-234-567-8902", "Los Angeles, CA", now);
        UserEntity analyst = seedUser(analystId, "data@insights.com", "Analyst@123", "Data Insights Team", "analyst", "+1-234-567-8907", "San Antonio, TX", now);

        Donation donation1 = new Donation();
        donation1.id = UUID.randomUUID().toString();
        donation1.item = "Fresh Vegetables";
        donation1.category = "vegetables";
        donation1.quantity = 50d;
        donation1.unit = "kg";
        donation1.status = "active";
        donation1.date = "2026-02-20";
        donation1.expiryDate = "2026-03-01";
        donation1.location = "Downtown";
        donation1.description = "Fresh seasonal vegetables";
        donation1.recipientOrg = "";
        donation1.donorId = donorId;
        donation1.createdAt = now.toString();
        donation1.updatedAt = now.toString();

        Donation donation2 = new Donation();
        donation2.id = UUID.randomUUID().toString();
        donation2.item = "Bread and Bakery";
        donation2.category = "bakery";
        donation2.quantity = 100d;
        donation2.unit = "items";
        donation2.status = "completed";
        donation2.date = "2026-02-19";
        donation2.expiryDate = "2026-02-21";
        donation2.location = "North Side";
        donation2.description = "Daily bakery surplus";
        donation2.recipientOrg = "Food Bank Central";
        donation2.donorId = donorId;
        donation2.createdAt = now.toString();
        donation2.updatedAt = now.toString();

        DataState state = new DataState();
        state.meta.put("createdAt", now.toString());
        state.meta.put("version", 1);
        state.users = new ArrayList<>(List.of(admin, donor, recipient, analyst));
        state.donations = new ArrayList<>(List.of(donation1, donation2));
        state.requests = new ArrayList<>();
        state.reports = new ArrayList<>();
        state.content.put("pages", new ArrayList<>());
        state.content.put("faqs", new ArrayList<>());
        state.content.put("announcements", new ArrayList<>());

        Map<String, Object> general = new LinkedHashMap<>();
        general.put("platformName", "Food Waste Platform");
        general.put("platformEmail", "admin@foodwaste.com");
        general.put("platformPhone", "+1-800-FOOD-AID");
        general.put("timezone", "America/New_York");
        general.put("language", "English");

        Map<String, Object> notifications = new LinkedHashMap<>();
        notifications.put("emailNotifications", true);
        notifications.put("smsNotifications", false);
        notifications.put("donationAlerts", true);
        notifications.put("approvalAlerts", true);
        notifications.put("weeklyReports", true);

        Map<String, Object> donations = new LinkedHashMap<>();
        donations.put("minDonationQty", "5");
        donations.put("maxDonationQty", "1000");
        donations.put("expiryWarningDays", "3");
        donations.put("autoApproveEnabled", false);
        donations.put("requireVerification", true);

        Map<String, Object> security = new LinkedHashMap<>();
        security.put("sessionTimeoutMinutes", 30);
        security.put("enforceStrongPasswords", true);
        security.put("twoFactorEnabled", false);

        state.settings.put("general", general);
        state.settings.put("notifications", notifications);
        state.settings.put("donations", donations);
        state.settings.put("security", security);
        return state;
    }

    private UserEntity seedUser(String id, String email, String password, String name, String role, String phone, String location, Instant now) {
        UserEntity user = new UserEntity();
        user.id = id;
        user.email = email;
        user.passwordHash = passwordService.hash(password);
        user.name = name;
        user.role = role;
        user.status = "active";
        user.phone = phone;
        user.location = location;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }
}
