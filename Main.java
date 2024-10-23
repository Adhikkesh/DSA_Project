package Draft_4;

import java.util.*;

class Interval implements Comparable<Interval> {
    int start, end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(Interval other) {
        return this.start < other.end && other.start < this.end;
    }

    @Override
    public String toString() {
        return "[" + start + ":00 - " + end + ":00]";
    }

    @Override
    public int compareTo(Interval other) {
        if (this.start != other.start) {
            return Integer.compare(this.start, other.start);
        }
        return Integer.compare(this.end, other.end);
    }
}

class Event {
    int eventID;
    String title;
    String description;
    String location;
    int priority;
    Interval interval;
    boolean isRecurring;
    int recurringDays;

    public Event(int eventID, String title, String description, String location, int priority, Interval interval, boolean isRecurring, int recurringDays) {
        this.eventID = eventID;
        this.title = title;
        this.description = description;
        this.location = location;
        this.priority = priority;
        this.interval = interval;
        this.isRecurring = isRecurring;
        this.recurringDays = recurringDays;
    }

    @Override
    public String toString() {
        return "ID: " + eventID + ", Title: " + title + ", Location: " + location + ", Time: " + interval + ", Priority: " + priority + ", Recurring: " + isRecurring;
    }
}

class IntervalTree {
    private Map<String, TreeMap<Interval, Event>> locationMap = new HashMap<>();

    public IntervalTree() {
        locationMap.put("Tech Fair Pavilion", new TreeMap<>());
        locationMap.put("Main Stage", new TreeMap<>());
        locationMap.put("Outdoor Garden", new TreeMap<>());
    }

    public boolean addEvent(Event event) {
        // Trim the location input to avoid issues with extra spaces
        String trimmedLocation = event.location.trim();

        // Check if the location exists in the map
        if (!locationMap.containsKey(trimmedLocation)) {
            System.out.println("Location '" + trimmedLocation + "' does not exist. Please select a valid location.");
            return false;
        }

        TreeMap<Interval, Event> locationEvents = locationMap.get(trimmedLocation);

        // Check for conflicts in the specific location
        for (Map.Entry<Interval, Event> entry : locationEvents.entrySet()) {
            if (entry.getKey().overlaps(event.interval)) {
                Event conflictingEvent = entry.getValue();
                if (event.priority < conflictingEvent.priority) {
                    // New event has higher priority, reschedule conflicting event
                    locationEvents.remove(entry.getKey());
                    locationEvents.put(event.interval, event);
                    return rescheduleEvent(conflictingEvent);
                } else {
                    // Conflict with higher or equal priority, reschedule the new event
                    return rescheduleEvent(event);
                }
            }
        }

        // No conflict, add the event
        locationEvents.put(event.interval, event);
        return true;
    }


    private boolean rescheduleEvent(Event event) {
        TreeMap<Interval, Event> locationEvents = locationMap.get(event.location);
        int newStart = Math.max(event.interval.start, getEarliestAvailableTime(event.interval.start, event.location));
        int newEnd = newStart + (event.interval.end - event.interval.start);

        while (newEnd <= 24) {
            Interval newInterval = new Interval(newStart, newEnd);
            boolean conflict = false;
            for (Interval interval : locationEvents.keySet()) {
                if (interval.overlaps(newInterval)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                event.interval = newInterval;
                locationEvents.put(newInterval, event);
                return true;
            }
            newStart = newEnd;
            newEnd = newStart + (event.interval.end - event.interval.start);
        }

        System.out.println("No available slot to reschedule event: " + event.title);
        return false;
    }

    private int getEarliestAvailableTime(int startTime, String location) {
        TreeMap<Interval, Event> locationEvents = locationMap.get(location);
        for (Interval interval : locationEvents.keySet()) {
            if (interval.start >= startTime) {
                return interval.end;
            }
        }
        return startTime;
    }

    public TreeMap<Interval, Event> getLocationEvents(String location) {
        return locationMap.getOrDefault(location, new TreeMap<>()); // Return the events for the specified location
    }

    public void displayEvents(String location) {
        TreeMap<Interval, Event> locationEvents = locationMap.get(location);
        if (locationEvents.isEmpty()) {
            System.out.println("No events scheduled in " + location + ".");
        } else {
            System.out.println("Scheduled Events in " + location + ":");
            for (Map.Entry<Interval, Event> entry : locationEvents.entrySet()) {
                System.out.println(entry.getValue());
            }
        }
    }

    public void displayFreeSlots(String location, int day) {
        TreeMap<Interval, Event> locationEvents = locationMap.get(location);
        System.out.println("Free slots for day " + day + " in " + location + ":");
        int previousEnd = 0;

        for (Interval interval : locationEvents.keySet()) {
            if (interval.start > previousEnd) {
                System.out.println("Free slot: " + previousEnd + ":00 - " + interval.start + ":00");
            }
            previousEnd = interval.end;
        }

        if (previousEnd < 24) {
            System.out.println("Free slot: " + previousEnd + ":00 - 24:00");
        }
    }

    public boolean modifyEvent(int eventID, Interval newInterval) {
        for (TreeMap<Interval, Event> events : locationMap.values()) {
            for (Event event : events.values()) {
                if (event.eventID == eventID) {
                    events.remove(event.interval);
                    event.interval = newInterval;
                    return addEvent(event);
                }
            }
        }
        System.out.println("Event not found.");
        return false;
    }

    public boolean deleteEvent(int eventID) {
        for (TreeMap<Interval, Event> events : locationMap.values()) {
            for (Event event : events.values()) {
                if (event.eventID == eventID) {
                    events.remove(event.interval);
                    System.out.println("Event deleted.");
                    return true;
                }
            }
        }
        System.out.println("Event not found.");
        return false;
    }
}

class EventManager {
    private static int nextEventID = 1;
    private IntervalTree[] days;

    public EventManager(int totalDays) {
        days = new IntervalTree[totalDays];
        for (int i = 0; i < totalDays; i++) {
            days[i] = new IntervalTree();
        }
    }

    public void addEvent(String title, String description, String location, int priority, Interval interval, boolean isRecurring, int recurringDays) {
        if (isRecurring) {
            for (int i = 0; i < recurringDays; i++) {
                Event event = new Event(nextEventID++, title, description, location, priority, interval, isRecurring, recurringDays);
                if (!days[i].addEvent(event)) {
                    System.out.println("Conflict detected for day " + (i + 1) + ".");
                }
            }
        } else {
            Event event = new Event(nextEventID++, title, description, location, priority, interval, isRecurring, 1);
            if (!days[0].addEvent(event)) {
                System.out.println("Conflict detected for day 1.");
            }
        }
    }

    public void displayFreeSlots(int day, String location) {
        days[day - 1].displayFreeSlots(location, day);
    }

    public void displayAllEvents() {
        String leftAlignFormat = "| %-10d | %-15s | %-10s | %-10s | %-8d | %-10s | %-8s |%n";

        for (int i = 0; i < days.length; i++) {
            IntervalTree dayTree = days[i];

            System.out.println("Day " + (i + 1) + ":");
            System.out.format("+------------+-----------------+------------+------------+----------+------------+----------+%n");
            System.out.format("| Event ID   | Title           | Start Time | End Time   | Priority | Recurring  | Duration |%n");
            System.out.format("+------------+-----------------+------------+------------+----------+------------+----------+%n");

            boolean hasEvents = false;
            // Iterate through each location and display events
            for (String location : new String[]{"Tech Fair Pavilion", "Main Stage", "Outdoor Garden"}) {
                TreeMap<Interval, Event> locationEvents = dayTree.getLocationEvents(location);
                for (Map.Entry<Interval, Event> entry : locationEvents.entrySet()) {
                    Event event = entry.getValue();
                    Interval interval = event.interval;

                    // Print the event in tabular format
                    System.out.printf(leftAlignFormat,
                            event.eventID,
                            event.title,
                            interval.start + ":00",
                            interval.end + ":00",
                            event.priority,
                            event.isRecurring ? "Yes" : "No",
                            (interval.end - interval.start) + "h"
                    );
                    hasEvents = true;
                }
            }

            if (!hasEvents) {
                System.out.println("| No events scheduled.                                                          |");
            }
            System.out.format("+------------+-----------------+------------+------------+----------+------------+----------+%n");
        }
    }


    public boolean modifyEvent(int eventID, Interval newInterval, int day) {
        return days[day - 1].modifyEvent(eventID, newInterval);
    }

    public boolean deleteEvent(int eventID, int day) {
        return days[day - 1].deleteEvent(eventID);
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Event Manager");
        System.out.println("============================");
        System.out.println("How many days would you like to schedule events for?");
        int days = scanner.nextInt();

        EventManager manager = new EventManager(days);
        int choice;

        do {
            System.out.println("\nMenu:");
            System.out.println("1. Add Event");
            System.out.println("2. Display Free Slots");
            System.out.println("3. Display All Events");
            System.out.println("4. Modify Event");
            System.out.println("5. Delete Event");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter title: ");
                    String title = scanner.nextLine();
                    System.out.print("Enter description: ");
                    String desc = scanner.nextLine();
                    System.out.print("Enter location (Tech Fair Pavilion/Main Stage/Outdoor Garden): ");
                    String location = scanner.nextLine().trim();
                    System.out.print("Enter priority (1-highest to 5-lowest): ");
                    int priority = scanner.nextInt();
                    System.out.print("Enter start time (24-hour format): ");
                    int startTime = scanner.nextInt();
                    System.out.print("Enter end time (24-hour format): ");
                    int endTime = scanner.nextInt();
                    System.out.print("Is this event recurring (true/false): ");
                    boolean isRecurring = scanner.nextBoolean();
                    int recurringDays = 1;
                    if (isRecurring) {
                        System.out.print("Enter number of days for recurrence: ");
                        recurringDays = scanner.nextInt();
                    }

                    manager.addEvent(title, desc, location, priority, new Interval(startTime, endTime), isRecurring, recurringDays);
                    break;

                case 2:
                    System.out.print("Enter day: ");
                    int day = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Enter location (Tech Fair Pavilion/Main Stage/Outdoor Garden): ");
                    String slotLocation = scanner.nextLine();
                    manager.displayFreeSlots(day, slotLocation);
                    break;

                case 3:
                    manager.displayAllEvents();
                    break;

                case 4:
                    System.out.print("Enter event ID: ");
                    int eventID = scanner.nextInt();
                    System.out.print("Enter day: ");
                    int modDay = scanner.nextInt();
                    System.out.print("Enter new start time: ");
                    int newStart = scanner.nextInt();
                    System.out.print("Enter new end time: ");
                    int newEnd = scanner.nextInt();

                    manager.modifyEvent(eventID, new Interval(newStart, newEnd), modDay);
                    break;

                case 5:
                    System.out.print("Enter event ID: ");
                    int delEventID = scanner.nextInt();
                    System.out.print("Enter day: ");
                    int delDay = scanner.nextInt();
                    manager.deleteEvent(delEventID, delDay);
                    break;

                case 6:
                    System.out.println("Exiting program...");
                    break;

                default:
                    System.out.println("Invalid choice, please try again.");
            }

        } while (choice != 6);

        scanner.close();
    }
}
