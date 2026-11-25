-- Ocupación y empresa pasan a ser opcionales; el ingreso mensual se captura como un rango (lista).
ALTER TABLE clientes ALTER COLUMN laboral_ocupacion DROP NOT NULL;
ALTER TABLE clientes ALTER COLUMN laboral_empresa DROP NOT NULL;
ALTER TABLE clientes ALTER COLUMN laboral_ingreso_mensual DROP NOT NULL;

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS laboral_rango_ingreso VARCHAR(30);

-- Clientes ya registrados: el rango se calcula a partir del ingreso que capturaron.
UPDATE clientes
SET laboral_rango_ingreso = CASE
        WHEN laboral_ingreso_mensual <= 5000 THEN 'HASTA_5000'
        WHEN laboral_ingreso_mensual <= 10000 THEN 'DE_5000_A_10000'
        WHEN laboral_ingreso_mensual <= 20000 THEN 'DE_10000_A_20000'
        WHEN laboral_ingreso_mensual <= 30000 THEN 'DE_20000_A_30000'
        ELSE 'MAS_DE_30000'
    END
WHERE laboral_rango_ingreso IS NULL AND laboral_ingreso_mensual IS NOT NULL;
