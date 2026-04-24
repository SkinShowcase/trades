ALTER TABLE trade_selection
    ALTER COLUMN steam_id TYPE VARCHAR(64),
    ADD COLUMN IF NOT EXISTS steam_id_enc TEXT;
