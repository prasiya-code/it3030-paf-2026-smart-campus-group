package com.smartcampus.backend.service;

import com.smartcampus.backend.entity.Booking;
import com.smartcampus.backend.entity.Comment;
import com.smartcampus.backend.entity.Resource;
import com.smartcampus.backend.entity.Ticket;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.BookingRepository;
import com.smartcampus.backend.repository.CommentRepository;
import com.smartcampus.backend.repository.ResourceRepository;
import com.smartcampus.backend.repository.TicketRepository;
import com.smartcampus.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    public Map<String, Object> getDashboardAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        // Total counts
        analytics.put("totalUsers", userRepository.count());
        analytics.put("totalResources", resourceRepository.count());
        analytics.put("totalBookings", bookingRepository.count());
        analytics.put("totalTickets", ticketRepository.count());
        analytics.put("totalComments", commentRepository.count());

        // Top resources by booking count
        analytics.put("topResources", getTopResources());

        // Peak booking hours
        analytics.put("peakBookingHours", getPeakBookingHours());

        // Recent activity
        analytics.put("recentActivity", getRecentActivity());

        // Ticket status distribution
        analytics.put("ticketStatusDistribution", getTicketStatusDistribution());

        // Booking status distribution
        analytics.put("bookingStatusDistribution", getBookingStatusDistribution());

        // User analytics
        analytics.put("userAnalytics", getUserAnalytics());

        // Ticket analytics
        analytics.put("ticketAnalytics", getTicketAnalytics());

        return analytics;
    }

    private List<Map<String, Object>> getTopResources() {
        List<Resource> resources = resourceRepository.findAll();
        List<Map<String, Object>> topResources = new ArrayList<>();

        for (Resource resource : resources) {
            List<Booking> bookings = bookingRepository.findByResourceId(resource.getId());
            Map<String, Object> resourceData = new HashMap<>();
            resourceData.put("id", resource.getId());
            resourceData.put("name", resource.getName());
            resourceData.put("type", resource.getType());
            resourceData.put("bookingCount", bookings.size());
            topResources.add(resourceData);
        }

        // Sort by booking count descending and limit to top 10
        return topResources.stream()
                .sorted((a, b) -> ((Integer) b.get("bookingCount")).compareTo((Integer) a.get("bookingCount")))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getPeakBookingHours() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<Integer, Integer> hourCounts = new HashMap<>();

        for (Booking booking : bookings) {
            if (booking.getStartTime() != null) {
                int hour = booking.getStartTime().getHour();
                hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);
            }
        }

        List<Map<String, Object>> peakHours = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : hourCounts.entrySet()) {
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", entry.getKey());
            hourData.put("count", entry.getValue());
            peakHours.add(hourData);
        }

        // Sort by count descending
        return peakHours.stream()
                .sorted((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentActivity() {
        List<Map<String, Object>> activities = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        // Recent bookings
        List<Booking> recentBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutoff))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        for (Booking booking : recentBookings) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "booking");
            activity.put("message", "New booking created");
            activity.put("timestamp", booking.getCreatedAt());
            if (booking.getUser() != null) {
                activity.put("user", booking.getUser().getEmail());
            }
            if (booking.getResource() != null) {
                activity.put("resource", booking.getResource().getName());
            }
            activities.add(activity);
        }

        // Recent tickets
        List<Ticket> recentTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(cutoff))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        for (Ticket ticket : recentTickets) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "ticket");
            activity.put("message", "New ticket created");
            activity.put("timestamp", ticket.getCreatedAt());
            if (ticket.getCreatedBy() != null) {
                activity.put("user", ticket.getCreatedBy().getEmail());
            }
            activity.put("description", ticket.getDescription());
            activities.add(activity);
        }

        // Sort all activities by timestamp
        return activities.stream()
                .sorted((a, b) -> ((LocalDateTime) b.get("timestamp")).compareTo((LocalDateTime) a.get("timestamp")))
                .limit(10)
                .collect(Collectors.toList());
    }

    private Map<String, Object> getTicketStatusDistribution() {
        List<Ticket> tickets = ticketRepository.findAll();
        Map<String, Integer> statusCounts = new HashMap<>();

        for (Ticket ticket : tickets) {
            String status = ticket.getStatus() != null ? ticket.getStatus().toString() : "UNKNOWN";
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        return new HashMap<>(statusCounts);
    }

    private Map<String, Object> getBookingStatusDistribution() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<String, Integer> statusCounts = new HashMap<>();

        for (Booking booking : bookings) {
            String status = booking.getStatus() != null ? booking.getStatus().toString() : "UNKNOWN";
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        return new HashMap<>(statusCounts);
    }

    private Map<String, Object> getUserAnalytics() {
        Map<String, Object> userAnalytics = new HashMap<>();
        List<User> users = userRepository.findAll();

        // User role distribution
        Map<String, Integer> roleDistribution = new HashMap<>();
        for (User user : users) {
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                for (com.smartcampus.backend.entity.Role role : user.getRoles()) {
                    String roleName = role.getName() != null ? role.getName().toString() : "USER";
                    roleDistribution.put(roleName, roleDistribution.getOrDefault(roleName, 0) + 1);
                }
            } else {
                roleDistribution.put("USER", roleDistribution.getOrDefault("USER", 0) + 1);
            }
        }
        userAnalytics.put("roleDistribution", roleDistribution);

        // User growth over last 7 days
        List<Map<String, Object>> userGrowth = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = users.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(date))
                    .count();
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", count);
            userGrowth.add(dayData);
        }
        userAnalytics.put("userGrowth", userGrowth);

        // Active users (created in last 30 days)
        long activeUsers = users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .count();
        userAnalytics.put("activeUsers", activeUsers);

        return userAnalytics;
    }

    private Map<String, Object> getTicketAnalytics() {
        Map<String, Object> ticketAnalytics = new HashMap<>();
        List<Ticket> tickets = ticketRepository.findAll();

        // Ticket trends over last 7 days
        List<Map<String, Object>> ticketTrends = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = tickets.stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(date))
                    .count();
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", count);
            ticketTrends.add(dayData);
        }
        ticketAnalytics.put("ticketTrends", ticketTrends);

        // Average resolution time for resolved tickets
        List<Ticket> resolvedTickets = tickets.stream()
                .filter(t -> "RESOLVED".equals(t.getStatus()) && t.getCreatedAt() != null && t.getUpdatedAt() != null)
                .collect(Collectors.toList());

        if (!resolvedTickets.isEmpty()) {
            double avgResolutionHours = resolvedTickets.stream()
                    .mapToLong(t -> ChronoUnit.HOURS.between(t.getCreatedAt(), t.getUpdatedAt()))
                    .average()
                    .orElse(0);
            ticketAnalytics.put("avgResolutionTime", avgResolutionHours);
        } else {
            ticketAnalytics.put("avgResolutionTime", 0);
        }

        // Open tickets count
        long openTickets = tickets.stream()
                .filter(t -> "OPEN".equals(t.getStatus()))
                .count();
        ticketAnalytics.put("openTickets", openTickets);

        // High priority tickets
        long highPriorityTickets = tickets.stream()
                .filter(t -> "HIGH".equals(t.getPriority()))
                .count();
        ticketAnalytics.put("highPriorityTickets", highPriorityTickets);

        return ticketAnalytics;
    }
}
