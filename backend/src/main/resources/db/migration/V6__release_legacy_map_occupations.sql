-- Corrige SOMENTE ocupacoes herdadas do PDF, importadas pelo Seed antigo.
-- Nao altera reservas reais, contratos, titulares nem fila de espera.
-- Mantem source_label e source_geometry para preservar o PDF original.
UPDATE booth
SET status = 'AVAILABLE',
    label = '',
    expires_at = NULL,
    version = version + 1
WHERE active = TRUE
  AND pavilion_id IN ('vitoria', 'iara', 'naval1', 'naval2')
  AND status = 'BLOCKED'
  AND owner_id IS NULL
  AND group_id IS NULL
  AND source_label IS NOT NULL
  AND TRIM(source_label) <> ''
  AND TRIM(label) = TRIM(source_label)
  AND NOT EXISTS (SELECT 1 FROM queue_entry q WHERE q.booth_id = booth.id);
