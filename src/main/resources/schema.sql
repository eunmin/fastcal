-- Calendars table
CREATE TABLE IF NOT EXISTS calendars (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    calendar_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(500) NOT NULL,
    description TEXT,
    color VARCHAR(20),
    timezone TEXT,
    ctag VARCHAR(100) NOT NULL,
    sync_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_calendars_user_calendar UNIQUE (user_id, calendar_id)
);

CREATE INDEX IF NOT EXISTS idx_calendars_user_id ON calendars(user_id);

-- Calendar events table
CREATE TABLE IF NOT EXISTS calendar_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    calendar_id VARCHAR(255) NOT NULL,
    uid VARCHAR(500) NOT NULL,
    ical_data TEXT NOT NULL,
    etag VARCHAR(100) NOT NULL,
    summary VARCHAR(1000),
    description TEXT,
    location VARCHAR(1000),
    dtstart TIMESTAMP,
    dtend TIMESTAMP,
    all_day BOOLEAN DEFAULT FALSE,
    rrule TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_events_user_calendar_uid UNIQUE (user_id, calendar_id, uid),
    CONSTRAINT fk_events_calendar FOREIGN KEY (user_id, calendar_id)
        REFERENCES calendars(user_id, calendar_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_events_user_calendar ON calendar_events(user_id, calendar_id);
CREATE INDEX IF NOT EXISTS idx_events_uid ON calendar_events(uid);
CREATE INDEX IF NOT EXISTS idx_events_dtstart ON calendar_events(dtstart);
CREATE INDEX IF NOT EXISTS idx_events_dtend ON calendar_events(dtend);
CREATE INDEX IF NOT EXISTS idx_events_time_range ON calendar_events(user_id, calendar_id, dtstart, dtend);

-- Sync changes table (for sync-collection REPORT)
CREATE TABLE IF NOT EXISTS sync_changes (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    calendar_id VARCHAR(255) NOT NULL,
    event_uid VARCHAR(500) NOT NULL,
    change_type VARCHAR(20) NOT NULL, -- CREATED, MODIFIED, DELETED
    sync_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_changes_calendar ON sync_changes(user_id, calendar_id);
CREATE INDEX IF NOT EXISTS idx_sync_changes_token ON sync_changes(user_id, calendar_id, sync_token);
CREATE INDEX IF NOT EXISTS idx_sync_changes_event_uid ON sync_changes(event_uid);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_calendars_updated_at ON calendars;
CREATE TRIGGER update_calendars_updated_at
    BEFORE UPDATE ON calendars
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_events_updated_at ON calendar_events;
CREATE TRIGGER update_events_updated_at
    BEFORE UPDATE ON calendar_events
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
