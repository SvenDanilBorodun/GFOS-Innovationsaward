-- GFOS Digital Idea Board - Datenbankschema
-- PostgreSQL 15+

-- Vorhandene Tabellen löschen, wenn sie existieren (für saubere Neuinstallation)
DROP TABLE IF EXISTS group_message_reads CASCADE;
DROP TABLE IF EXISTS group_messages CASCADE;
DROP TABLE IF EXISTS group_members CASCADE;
DROP TABLE IF EXISTS idea_groups CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS checklist_items CASCADE;
DROP TABLE IF EXISTS user_badges CASCADE;
DROP TABLE IF EXISTS badges CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS survey_votes CASCADE;
DROP TABLE IF EXISTS survey_options CASCADE;
DROP TABLE IF EXISTS surveys CASCADE;
DROP TABLE IF EXISTS comment_reactions CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS likes CASCADE;
DROP TABLE IF EXISTS file_attachments CASCADE;
DROP TABLE IF EXISTS idea_tags CASCADE;
DROP TABLE IF EXISTS ideas CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Hinweis: VARCHAR statt PostgreSQL Enum-Typen für JPA-Kompatibilität verwendet
-- Die Enum-Werte werden auf Anwendungsebene validiert

-- =====================================================
-- BENUTZER-TABELLE
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE' CHECK (role IN ('EMPLOYEE', 'PROJECT_MANAGER', 'ADMIN')),
    avatar_url VARCHAR(500),
    xp_points INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- =====================================================
-- IDEEN-TABELLE
-- =====================================================
CREATE TABLE ideas (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONCEPT' CHECK (status IN ('CONCEPT', 'IN_PROGRESS', 'COMPLETED')),
    progress_percentage INTEGER NOT NULL DEFAULT 0 CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    like_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    view_count INTEGER NOT NULL DEFAULT 0,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ideas_author ON ideas(author_id);
CREATE INDEX idx_ideas_category ON ideas(category);
CREATE INDEX idx_ideas_status ON ideas(status);
CREATE INDEX idx_ideas_created_at ON ideas(created_at DESC);
CREATE INDEX idx_ideas_like_count ON ideas(like_count DESC);

-- =====================================================
-- IDEEN-TAGS-TABELLE
-- =====================================================
CREATE TABLE idea_tags (
    id BIGSERIAL PRIMARY KEY,
    idea_id BIGINT NOT NULL REFERENCES ideas(id) ON DELETE CASCADE,
    tag_name VARCHAR(50) NOT NULL,
    UNIQUE(idea_id, tag_name)
);

CREATE INDEX idx_idea_tags_idea ON idea_tags(idea_id);
CREATE INDEX idx_idea_tags_name ON idea_tags(tag_name);

-- =====================================================
-- DATEIANHÄNGE-TABELLE
-- =====================================================
CREATE TABLE file_attachments (
    id BIGSERIAL PRIMARY KEY,
    idea_id BIGINT NOT NULL REFERENCES ideas(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_file_attachments_idea ON file_attachments(idea_id);

-- =====================================================
-- LIKES-TABELLE
-- =====================================================
CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    idea_id BIGINT NOT NULL REFERENCES ideas(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, idea_id)
);

CREATE INDEX idx_likes_user ON likes(user_id);
CREATE INDEX idx_likes_idea ON likes(idea_id);
CREATE INDEX idx_likes_created_at ON likes(created_at);

-- =====================================================
-- KOMMENTARE-TABELLE
-- =====================================================
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    idea_id BIGINT NOT NULL REFERENCES ideas(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(200) NOT NULL,
    reaction_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comments_idea ON comments(idea_id);
CREATE INDEX idx_comments_author ON comments(author_id);
CREATE INDEX idx_comments_created_at ON comments(created_at DESC);

-- =====================================================
-- CHECKLISTEN-ELEMENTE-TABELLE
-- =====================================================
CREATE TABLE checklist_items (
    id BIGSERIAL PRIMARY KEY,
    idea_id BIGINT NOT NULL REFERENCES ideas(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ordinal_position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_checklist_items_idea ON checklist_items(idea_id);
CREATE INDEX idx_checklist_items_position ON checklist_items(idea_id, ordinal_position);

-- =====================================================
-- KOMMENTAR-REAKTIONEN-TABELLE
-- =====================================================
CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(comment_id, user_id, emoji)
);

CREATE INDEX idx_comment_reactions_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reactions_user ON comment_reactions(user_id);

-- =====================================================
-- UMFRAGEN-TABELLE
-- =====================================================
CREATE TABLE surveys (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question VARCHAR(500) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    allow_multiple_votes BOOLEAN NOT NULL DEFAULT FALSE,
    total_votes INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_surveys_creator ON surveys(creator_id);
CREATE INDEX idx_surveys_active ON surveys(is_active);
CREATE INDEX idx_surveys_created_at ON surveys(created_at DESC);

-- =====================================================
-- UMFRAGE-OPTIONEN-TABELLE
-- =====================================================
CREATE TABLE survey_options (
    id BIGSERIAL PRIMARY KEY,
    survey_id BIGINT NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    option_text VARCHAR(200) NOT NULL,
    vote_count INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_survey_options_survey ON survey_options(survey_id);

-- =====================================================
-- UMFRAGE-ABSTIMMUNGEN-TABELLE
-- =====================================================
CREATE TABLE survey_votes (
    id BIGSERIAL PRIMARY KEY,
    survey_id BIGINT NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES survey_options(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(survey_id, user_id, option_id)
);

CREATE INDEX idx_survey_votes_survey ON survey_votes(survey_id);
CREATE INDEX idx_survey_votes_user ON survey_votes(user_id);

-- =====================================================
-- ABZEICHEN-TABELLE
-- =====================================================
CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    description VARCHAR(500) NOT NULL,
    icon VARCHAR(100) NOT NULL,
    criteria VARCHAR(500),
    xp_reward INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- BENUTZER-ABZEICHEN-TABELLE
-- =====================================================
CREATE TABLE user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id BIGINT NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, badge_id)
);

CREATE INDEX idx_user_badges_user ON user_badges(user_id);
CREATE INDEX idx_user_badges_badge ON user_badges(badge_id);

-- =====================================================
-- AUDIT-PROTOKOLLE-TABELLE
-- =====================================================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'LOGIN', 'LOGOUT')),
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =====================================================
-- BENACHRICHTIGUNGEN-TABELLE
-- =====================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('LIKE', 'COMMENT', 'REACTION', 'STATUS_CHANGE', 'BADGE_EARNED', 'LEVEL_UP', 'MENTION', 'MESSAGE')),
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    link VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- =====================================================
-- NACHRICHTEN-TABELLE (Benutzer-zu-Benutzer-Nachrichten)
-- =====================================================
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    idea_id BIGINT REFERENCES ideas(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_different_users CHECK (sender_id != recipient_id)
);

CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_recipient ON messages(recipient_id);
CREATE INDEX idx_messages_idea ON messages(idea_id);
CREATE INDEX idx_messages_conversation ON messages(LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id));
CREATE INDEX idx_messages_unread ON messages(recipient_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);

-- =====================================================
-- IDEEN-GRUPPEN-TABELLE (Automatisch erstellt, wenn eine Idee erstellt wird)
-- =====================================================
CREATE TABLE idea_groups (
    id BIGSERIAL PRIMARY KEY,
    idea_id BIGINT NOT NULL UNIQUE REFERENCES ideas(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_idea_groups_idea ON idea_groups(idea_id);
CREATE INDEX idx_idea_groups_created_by ON idea_groups(created_by);

-- =====================================================
-- GRUPPENMITGLIEDER-TABELLE (Verbindungstabelle für Gruppenmitgliedschaft)
-- =====================================================
CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES idea_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('CREATOR', 'MEMBER')),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);

-- =====================================================
-- GRUPPENNACHRICHTEN-TABELLE (Nachrichten innerhalb von Ideengruppen)
-- =====================================================
CREATE TABLE group_messages (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES idea_groups(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_group_messages_group ON group_messages(group_id);
CREATE INDEX idx_group_messages_sender ON group_messages(sender_id);
CREATE INDEX idx_group_messages_created_at ON group_messages(created_at DESC);

-- =====================================================
-- GRUPPENNACHRICHT-LESESTATUS-TABELLE (Verfolgung, wer welche Nachrichten gelesen hat)
-- =====================================================
CREATE TABLE group_message_reads (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES group_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, user_id)
);

CREATE INDEX idx_group_message_reads_message ON group_message_reads(message_id);
CREATE INDEX idx_group_message_reads_user ON group_message_reads(user_id);

-- =====================================================
-- FUNKTIONEN UND TRIGGER
-- =====================================================

-- Funktion zum Aktualisieren des updated_at-Zeitstempels
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger für updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ideas_updated_at
    BEFORE UPDATE ON ideas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_checklist_items_updated_at
    BEFORE UPDATE ON checklist_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_idea_groups_updated_at
    BEFORE UPDATE ON idea_groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Funktion zum Aktualisieren der Ideen-Like-Zählung
CREATE OR REPLACE FUNCTION update_idea_like_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE ideas SET like_count = like_count + 1 WHERE id = NEW.idea_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE ideas SET like_count = like_count - 1 WHERE id = OLD.idea_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_like_count
    AFTER INSERT OR DELETE ON likes
    FOR EACH ROW EXECUTE FUNCTION update_idea_like_count();

-- Funktion zum Aktualisieren der Ideen-Kommentar-Zählung
CREATE OR REPLACE FUNCTION update_idea_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE ideas SET comment_count = comment_count + 1 WHERE id = NEW.idea_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE ideas SET comment_count = comment_count - 1 WHERE id = OLD.idea_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_comment_count
    AFTER INSERT OR DELETE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_idea_comment_count();

-- Funktion zum Aktualisieren der Kommentar-Reaktions-Zählung
CREATE OR REPLACE FUNCTION update_comment_reaction_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE comments SET reaction_count = reaction_count + 1 WHERE id = NEW.comment_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE comments SET reaction_count = reaction_count - 1 WHERE id = OLD.comment_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_reaction_count
    AFTER INSERT OR DELETE ON comment_reactions
    FOR EACH ROW EXECUTE FUNCTION update_comment_reaction_count();

-- Funktion zum Aktualisieren der Umfrage-Abstimmungs-Zählung
CREATE OR REPLACE FUNCTION update_survey_vote_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE survey_options SET vote_count = vote_count + 1 WHERE id = NEW.option_id;
        UPDATE surveys SET total_votes = total_votes + 1 WHERE id = NEW.survey_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE survey_options SET vote_count = vote_count - 1 WHERE id = OLD.option_id;
        UPDATE surveys SET total_votes = total_votes - 1 WHERE id = OLD.survey_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_vote_count
    AFTER INSERT OR DELETE ON survey_votes
    FOR EACH ROW EXECUTE FUNCTION update_survey_vote_count();

-- =====================================================
-- SEED-DATEN
-- =====================================================

-- Admin-Benutzer einfügen (Passwort: admin123)
-- Hash mit BCrypt-Kosten faktor 12 generiert, verifiziert, um mit PasswordUtil-Implementierung übereinzustimmen
INSERT INTO users (username, email, password_hash, first_name, last_name, role, xp_points, level) VALUES
    ('admin', 'admin@gfos.com', '$2a$12$MMbkxZQfQePt3aApd8bCsuSv0U7pT54rR708XyXXNq9gcnfjrsTBy', 'System', 'Administrator', 'ADMIN', 0, 1);

-- Test-Benutzer einfügen (Passwort: password123)
-- Hash mit BCrypt-Kosten faktor 12 generiert, verifiziert, um mit PasswordUtil-Implementierung übereinzustimmen
INSERT INTO users (username, email, password_hash, first_name, last_name, role, xp_points, level) VALUES
    ('jsmith', 'john.smith@gfos.com', '$2a$12$9qf4aU3aQ.iXkYJYAea3deFQODQxKIwpV63Vz7p6CuCya.s696RXG', 'John', 'Smith', 'EMPLOYEE', 0, 1),
    ('mwilson', 'mary.wilson@gfos.com', '$2a$12$9qf4aU3aQ.iXkYJYAea3deFQODQxKIwpV63Vz7p6CuCya.s696RXG', 'Mary', 'Wilson', 'PROJECT_MANAGER', 0, 1),
    ('tjohnson', 'tom.johnson@gfos.com', '$2a$12$9qf4aU3aQ.iXkYJYAea3deFQODQxKIwpV63Vz7p6CuCya.s696RXG', 'Tom', 'Johnson', 'EMPLOYEE', 0, 1);

-- =====================================================
-- ABZEICHEN (Badges) - System-Konfiguration
-- =====================================================
INSERT INTO badges (name, display_name, description, icon, criteria, xp_reward, is_active) VALUES
    ('first_idea', 'Ideenstarter', 'Erste Idee eingereicht', 'lightbulb', 'Reiche deine erste Idee ein', 25, true),
    ('popular', 'Beliebt', '10 Likes erhalten', 'heart', 'Erhalte insgesamt 10 Likes auf deine Ideen', 50, true),
    ('commentator', 'Kommentator', '50 Kommentare geschrieben', 'chat-bubble', 'Schreibe 50 Kommentare', 75, true);