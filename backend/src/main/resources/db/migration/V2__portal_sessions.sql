-- Escopo de empresa por sessão; sessões anteriores exigem novo login.
ALTER TABLE auth_session ADD COLUMN portal VARCHAR(24) CHECK (portal IN ('navalshore','nn','portos'));
