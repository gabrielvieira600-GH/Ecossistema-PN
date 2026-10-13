-- V5: booth corner quantities. Compatible with PostgreSQL and H2 test database.
-- Apply only to Navalshore; never change stand ownership or existing commercial requests.
ALTER TABLE booth ADD COLUMN corners INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booth ADD CONSTRAINT booth_corners_range CHECK (corners BETWEEN 0 AND 20);

-- Code aliases include A1/A01/A-01, and K61/K061/K06-1.
-- Values below come from the exhibitor-provided corner list.
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A01', 'A1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A02', 'A2');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A03', 'A3');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A04', 'A4');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A05', 'A5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A07', 'A7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A08', 'A8');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A09', 'A9');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A010', 'A10');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A010A', 'A10A');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A011', 'A11');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A012', 'A12');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A013', 'A13');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A015', 'A15');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A017', 'A17');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A018', 'A18');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A019', 'A19');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A020', 'A20');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A021', 'A21');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('A022', 'A22');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B01', 'B1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B02', 'B2');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B03', 'B3');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B04', 'B4');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B05', 'B5');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B06', 'B6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B07', 'B7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B08', 'B8');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B081', 'B81');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B09', 'B9');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B010', 'B10');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B011', 'B11');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B012', 'B12');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B013', 'B13');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B014', 'B14');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B015', 'B15');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B016', 'B16');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B017', 'B17');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B018', 'B18');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B019', 'B19');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B020', 'B20');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B021', 'B21');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('B022', 'B22');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C01', 'C1');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C02', 'C2');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C03', 'C3');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C04', 'C4');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C05', 'C5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C06', 'C6');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C07', 'C7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C08', 'C8');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C09', 'C9');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C010', 'C10');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C011', 'C11');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C013', 'C13');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C014', 'C14');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C015', 'C15');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C016', 'C16');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C018', 'C18');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C019', 'C19');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C021', 'C21');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('C022', 'C22');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('D02', 'D2');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('D03', 'D3');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('D04', 'D4');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('D05', 'D5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('E01', 'E1');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('E05', 'E5');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('F01', 'F1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('F05', 'F5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('F06', 'F6');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G01', 'G1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G02', 'G2');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G03', 'G3');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G04', 'G4');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G05', 'G5');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G06', 'G6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G07', 'G7');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G08', 'G8');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G09', 'G9');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G010', 'G10');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G011', 'G11');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G012', 'G12');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G013', 'G13');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G014', 'G14');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G015', 'G15');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('G016', 'G16');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H01', 'H1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H02', 'H2');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H03', 'H3');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H04', 'H4');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H05', 'H5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H06', 'H6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H07', 'H7');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H08', 'H8');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H09', 'H9');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H010', 'H10');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H011', 'H11');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('H012', 'H12');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K01', 'K1');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K03', 'K3');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K05', 'K5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K06', 'K6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K061', 'K61');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K07', 'K7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K08', 'K8');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K09', 'K9');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K010', 'K10');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K011', 'K11');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K012', 'K12');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K014', 'K14');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K016', 'K16');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K017', 'K17');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K018', 'K18');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K019', 'K19');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('K020', 'K20');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L01', 'L1');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L03', 'L3');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L04', 'L4');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L05', 'L5');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L06', 'L6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L07', 'L7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L08', 'L8');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L09', 'L9');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L010', 'L10');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L011', 'L11');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L012', 'L12');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L014', 'L14');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L016', 'L16');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L018', 'L18');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L020', 'L20');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L022', 'L22');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L023', 'L23');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L024', 'L24');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L025', 'L25');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L026', 'L26');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L027', 'L27');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L028', 'L28');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('L029', 'L29');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M01', 'M1');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M02', 'M2');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M03', 'M3');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M04', 'M4');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M05', 'M5');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M06', 'M6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M07', 'M7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M08', 'M8');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M09', 'M9');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M010', 'M10');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M011', 'M11');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M012', 'M12');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M013', 'M13');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M014', 'M14');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('M016', 'M16');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N01', 'N1');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N02', 'N2');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N03', 'N3');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N04', 'N4');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N05', 'N5');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N06', 'N6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N07', 'N7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N09', 'N9');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N011', 'N11');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N013', 'N13');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N014', 'N14');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N015', 'N15');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N016', 'N16');
UPDATE booth SET corners = 4
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('N017', 'N17');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O01', 'O1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O02', 'O2');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O03', 'O3');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O031', 'O31');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O04', 'O4');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O05', 'O5');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O06', 'O6');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O08', 'O8');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O010', 'O10');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O014', 'O14');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O016', 'O16');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O018', 'O18');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O020', 'O20');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O022', 'O22');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O024', 'O24');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('O025', 'O25');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('P01', 'P1');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('P02', 'P2');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('P03', 'P3');
UPDATE booth SET corners = 2
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('P04', 'P4');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('X01', 'X1');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('X02', 'X2');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('X03', 'X3');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('X05', 'X5');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('X06', 'X6');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z01', 'Z1');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z02', 'Z2');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z03', 'Z3');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z04', 'Z4');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z05', 'Z5');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z06', 'Z6');
UPDATE booth SET corners = 0
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z07', 'Z7');
UPDATE booth SET corners = 1
WHERE pavilion_id IN (SELECT p.id FROM pavilion p JOIN fair f ON f.id=p.fair_id WHERE LOWER(f.id) LIKE 'naval%')
  AND UPPER(REPLACE(REPLACE(code, '-', ''), ' ', '')) IN ('Z08', 'Z8');
