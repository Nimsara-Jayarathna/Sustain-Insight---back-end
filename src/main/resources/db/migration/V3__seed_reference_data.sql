-- Seed the controlled sustainability taxonomy required by the application.
-- Categories are reference data and are intentionally managed by Flyway.
-- IDs are not hard-coded: application logic resolves categories by name and then
-- passes the actual database IDs to the synthesis prompt.

INSERT INTO categories (name) VALUES
    ('Climate Change'),
    ('Renewable Energy'),
    ('Biodiversity'),
    ('Pollution & Waste'),
    ('Sustainable Business & ESG'),
    ('Green Technology'),
    ('Policy & Regulation'),
    ('General Sustainability')
ON CONFLICT (name) DO NOTHING;

-- These are provider-level fallbacks. Publisher/source names discovered from
-- incoming raw articles are added dynamically by SourceService.
INSERT INTO sources (name) VALUES
    ('The Guardian'),
    ('NewsAPI')
ON CONFLICT (name) DO NOTHING;
