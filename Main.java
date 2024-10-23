package Draft_5_jai;

import java.util.*;

class EventManager {
    private static int nextEventID = 1;
    private IntervalTree[] days;
    private int[] dailyTimeLimits; // Store daily time limits
    private int[] usedTime; // Store used time for each day

    public EventManager(int totalDays) {
        days = new IntervalTree[totalDays];
        dailyTimeLimits = new int[totalDays];
        usedTime = new int[totalDays];
        for (int i = 0; i < totalDays; i++) {
            days[i] = new IntervalTree();
        }
    }

    public void setDailyTimeLimit(int day, int limit) {
        dailyTimeLimits[day] = limit; // Set the daily time limit
    }

    public void addEvent(String title, String description, int priority, Interval interval, boolean isRecurring, int recurringDays) {
        if (isRecurring) {
            boolean eventAdded = false; // Flag to track if at least one event was added successfully

            for (int i = 0; i < recurringDays; i++) {
                if (usedTime[i] + (interval.end - interval.start) > dailyTimeLimits[i]) {
                    System.out.println("Time limit reached for day " + (i + 1) + ", trying to reschedule to the next day.");
                    // Try to reschedule to the next day
                    if (!rescheduleToNextDay(title, description, priority, interval, i)) {
                        System.out.println("Could not schedule event: " + title + " on any day.");
                        return; // Exit if rescheduling fails
                    }
                } else {
                    Event event = new Event(nextEventID++, title, description, priority, interval, isRecurring, recurringDays);
                    // Attempt to add the event
                    if (!days[i].addEvent(event)) {
                        System.out.println("Conflict detected for day " + (i + 1) + ", attempting to resolve based on priority.");
                        if (!resolveConflictByPriority(event, i)) {
                            System.out.println("Conflict could not be resolved for event: " + title);
                        }
                    } else {
                        usedTime[i] += (interval.end - interval.start); // Update used time
                        eventAdded = true;
                    }
                }
            }

            // Print a single confirmation message if at least one event was added
            if (eventAdded) {
                System.out.println("Event added: " + title + " (Recurring for " + recurringDays + " days)");
            }
        } else {
            // For non-recurring events, prompt the user to choose a specific day
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the day number to schedule the event (1 to " + days.length + "): ");
            int day = scanner.nextInt();

            // Validate the day input
            if (day >= 1 && day <= days.length) {
                if (usedTime[day - 1] + (interval.end - interval.start) > dailyTimeLimits[day - 1]) {
                    System.out.println("Time limit reached for day " + day + ". Cannot add event.");
                } else {
                    Event event = new Event(nextEventID++, title, description, priority, interval, isRecurring, 1);
                    if (!days[day - 1].addEvent(event)) {
                        System.out.println("Conflict detected for day " + day + ", attempting to resolve based on priority.");
                        if (!resolveConflictByPriority(event, day - 1)) {
                            System.out.println("Conflict could not be resolved for event: " + title);
                        }
                    } else {
                        usedTime[day - 1] += (interval.end - interval.start); // Update used time
                        System.out.println("Event added: " + title + " on day " + day);
                    }
                }
            } else {
                System.out.println("Invalid day number.");
            }
        }
    }


    private boolean resolveConflictByPriority(Event newEvent, int day) {
        // Retrieve the existing events for the day
        TreeMap<Interval, Event> existingEvents = days[day].getIntervalTree();
        Event eventToRemove = null;

        for (Map.Entry<Interval, Event> entry : existingEvents.entrySet()) {
            Event existingEvent = entry.getValue();
            if (existingEvent.priority > newEvent.priority) {
                // Try to reschedule the new event after the existing event
                if (!days[day].rescheduleEvent(newEvent, existingEvent.interval.end)) {
                    System.out.println("Could not reschedule event: " + newEvent.title);
                    return false; // Conflict could not be resolved
                }
                return true; // Rescheduled successfully
            } else if (existingEvent.priority <= newEvent.priority) {
                // Mark the existing event for removal if it has lower or equal priority
                eventToRemove = existingEvent;
            }
        }

        // If the existing event has lower priority, we should attempt to remove it
        if (eventToRemove != null) {
            // Attempt to find the next free slot for the lower priority event
            int duration = newEvent.interval.end - newEvent.interval.start;
            for (int i = day; i < days.length; i++) {
                // Check if there's enough time on this day
                if (usedTime[i] + duration <= dailyTimeLimits[i]) {
                    // Reschedule the lower priority event to the next available slot
                    if (days[i].addEvent(eventToRemove)) {
                        usedTime[i] += duration; // Update used time
                        System.out.println("Event " + eventToRemove.title + " rescheduled to day " + (i + 1));
                        // Now we can add the new event
                        if (days[day].addEvent(newEvent)) {
                            usedTime[day] += duration; // Update used time for the new event
                            return true;
                        }
                    }
                }
            }
        }
        return false; // No resolution was found
    }

    private boolean rescheduleToNextDay(String title, String description, int priority, Interval interval, int currentDay) {
        for (int i = currentDay + 1; i < days.length; i++) {
            if (usedTime[i] + (interval.end - interval.start) <= dailyTimeLimits[i]) {
                Event event = new Event(nextEventID++, title, description, priority, interval, true, 1);
                if (days[i].addEvent(event)) {
                    usedTime[i] += (interval.end - interval.start); // Update used time
                    System.out.println("Event " + title + " rescheduled to day " + (i + 1));
                    return true; // Event successfully rescheduled
                }
            }
        }
        return false; // No available slot found in subsequent days
    }

    public void removeEvent(int eventID, int day) {
        if (day < 1 || day > days.length) {
            System.out.println("Invalid day number.");
            return;
        }

        // Try to remove the event from the specified day's IntervalTree
        boolean removed = days[day - 1].removeEvent(eventID);
        if (removed) {
            // Reduce the used time accordingly
            System.out.println("Event with ID " + eventID + " removed from day " + day);
            // Recalculate the used time for that day (optional)
            recalculateUsedTime(day - 1);
        } else {
            System.out.println("Event with ID " + eventID + " not found on day " + day);
        }
    }

    private void recalculateUsedTime(int day) {
        usedTime[day] = 0; // Reset used time for the day
        TreeMap<Interval, Event> events = days[day].getIntervalTree();
        for (Map.Entry<Interval, Event> entry : events.entrySet()) {
            Event event = entry.getValue();
            usedTime[day] += (event.interval.end - event.interval.start);
        }
    }

    public void modifyEvent(int eventID, int day, String newTitle, String newDescription, int newPriority, Interval newInterval) {
        if (day < 1 || day > days.length) {
            System.out.println("Invalid day number.");
            return;
        }

        Event existingEvent = days[day - 1].getEventById(eventID);
        if (existingEvent == null) {
            System.out.println("Event with ID " + eventID + " not found on day " + day);
            return;
        }

        // Calculate the time change for the event
        int originalDuration = existingEvent.interval.end - existingEvent.interval.start;
        int newDuration = newInterval.end - newInterval.start;

        // Update used time before modification
        usedTime[day - 1] -= originalDuration;

        // Check if the new interval can fit within the daily limit
        if (usedTime[day - 1] + newDuration > dailyTimeLimits[day - 1]) {
            System.out.println("Time limit exceeded for day " + day + ". Cannot modify event.");
            // Restore the original used time in case of failure
            usedTime[day - 1] += originalDuration;
            return;
        }

        // Remove the existing event and create a new one
        days[day - 1].removeEvent(eventID);
        Event modifiedEvent = new Event(eventID, newTitle, newDescription, newPriority, newInterval, existingEvent.isRecurring, existingEvent.recurringDays);

        if (!days[day - 1].addEvent(modifiedEvent)) {
            System.out.println("Conflict detected for modified event: " + newTitle);
            // Restore the original used time if adding fails
            usedTime[day - 1] += originalDuration;
            // Attempt to resolve conflict here if needed
            return;
        }

        // Update used time after successful modification
        usedTime[day - 1] += newDuration;
        System.out.println("Event modified: " + modifiedEvent.title + " on day " + day);
    }

    public void displayEvents(int day) {
        if (day < 1 || day > days.length) {
            System.out.println("Invalid day number.");
            return;
        }

        TreeMap<Interval, Event> events = days[day - 1].getIntervalTree();
        if (events.isEmpty()) {
            System.out.println("No events scheduled for day " + day);
            return;
        }

        System.out.printf("%-10s %-20s %-15s %-15s %-10s%n", "Event ID", "Title", "Description", "Priority", "Time");
        System.out.println("--------------------------------------------------------------------------------");
        for (Map.Entry<Interval, Event> entry : events.entrySet()) {
            Event event = entry.getValue();
            System.out.printf("%-10d %-20s %-15s %-15d %-10d - %-10d%n",
                    event.id, event.title, event.description, event.priority, event.interval.start, event.interval.end);
        }
    }


}

// Interval class to represent the start and end times of events
class Interval {
    int start;
    int end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }
}

// Event class to represent an event
class Event {
    int id;
    String title;
    String description;
    int priority;
    Interval interval;
    boolean isRecurring;
    int recurringDays;

    public Event(int id, String title, String description, int priority, Interval interval, boolean isRecurring, int recurringDays) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.interval = interval;
        this.isRecurring = isRecurring;
        this.recurringDays = recurringDays;
    }
}

class IntervalTree {
    private TreeMap<Interval, Event> eventMap;

    public IntervalTree() {
        this.eventMap = new TreeMap<>(Comparator.comparingInt(interval -> interval.start));
    }

    public boolean addEvent(Event event) {
        Interval interval = event.interval;

        // Check for conflicts with existing events
        for (Interval existingInterval : eventMap.keySet()) {
            // Check if the new event overlaps with existing events
            if (overlaps(existingInterval, interval)) {
                return false; // Conflict detected
            }
        }

        eventMap.put(interval, event);
        return true; // Event added successfully
    }

    public boolean removeEvent(int eventID) {
        // Remove the event based on its ID
        for (Map.Entry<Interval, Event> entry : eventMap.entrySet()) {
            if (entry.getValue().id == eventID) {
                eventMap.remove(entry.getKey());
                return true; // Event removed successfully
            }
        }
        return false; // Event not found
    }

    public Event getEventById(int eventID) {
        // Retrieve an event by its ID
        for (Event event : eventMap.values()) {
            if (event.id == eventID) {
                return event;
            }
        }
        return null; // Event not found
    }

    public TreeMap<Interval, Event> getIntervalTree() {
        return eventMap; // Return the internal map for external access
    }

    public boolean rescheduleEvent(Event event, int newStart) {
        int duration = event.interval.end - event.interval.start;
        Interval newInterval = new Interval(newStart, newStart + duration);

        // Check for conflicts with the new interval
        for (Interval existingInterval : eventMap.keySet()) {
            if (overlaps(existingInterval, newInterval)) {
                return false; // Conflict detected, cannot reschedule
            }
        }

        // If no conflict, update the event and add it
        eventMap.remove(event.interval); // Remove old interval
        event.interval = newInterval; // Update to new interval
        eventMap.put(newInterval, event); // Add to map
        return true; // Event rescheduled successfully
    }

    private boolean overlaps(Interval a, Interval b) {
        return a.start < b.end && b.start < a.end; // Check for overlap
    }
}


// Main class to run the EventManager
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of days: ");
        int totalDays = scanner.nextInt();
        EventManager eventManager = new EventManager(totalDays);

        // Set daily time limits
        for (int i = 0; i < totalDays; i++) {
            System.out.print("Enter time limit for day " + (i + 1) + ": ");
            int limit = scanner.nextInt();
            eventManager.setDailyTimeLimit(i, limit);
        }

        // User input for adding/modifying/removing events
        while (true) {
            System.out.println("1. Add Event\n2. Modify Event\n3. Remove Event\n4. View Events\n5. Exit");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter event title: ");
                    String title = scanner.nextLine();
                    System.out.print("Enter event description: ");
                    String description = scanner.nextLine();
                    System.out.print("Enter event priority: ");
                    int priority = scanner.nextInt();
                    System.out.print("Enter event start time: ");
                    int start = scanner.nextInt();
                    System.out.print("Enter event end time: ");
                    int end = scanner.nextInt();
                    System.out.print("Is the event recurring? (true/false): ");
                    boolean isRecurring = scanner.nextBoolean();
                    int recurringDays = 1;
                    if (isRecurring) {
                        System.out.print("Enter number of recurring days: ");
                        recurringDays = scanner.nextInt();
                    }
                    eventManager.addEvent(title, description, priority, new Interval(start, end), isRecurring, recurringDays);
                    break;

                case 2:
                    System.out.print("Enter event ID to modify: ");
                    int eventIDToModify = scanner.nextInt();
                    System.out.print("Enter day number of the event: ");
                    int dayToModify = scanner.nextInt();
                    System.out.print("Enter new event title: ");
                    String newTitle = scanner.nextLine();
                    System.out.print("Enter new event description: ");
                    String newDescription = scanner.nextLine();
                    System.out.print("Enter new event priority: ");
                    int newPriority = scanner.nextInt();
                    System.out.print("Enter new event start time: ");
                    int newStart = scanner.nextInt();
                    System.out.print("Enter new event end time: ");
                    int newEnd = scanner.nextInt();
                    eventManager.modifyEvent(eventIDToModify, dayToModify, newTitle, newDescription, newPriority, new Interval(newStart, newEnd));
                    break;

                case 3:
                    System.out.print("Enter event ID to remove: ");
                    int eventIDToRemove = scanner.nextInt();
                    System.out.print("Enter day number of the event: ");
                    int dayToRemove = scanner.nextInt();
                    eventManager.removeEvent(eventIDToRemove, dayToRemove);
                    break;

                case 4:
                    System.out.print("Enter day number to view events: ");
                    int dayToView = scanner.nextInt();
                    eventManager.displayEvents(dayToView);
                    break;


                case 5:
                    System.out.println("Exiting program.");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
