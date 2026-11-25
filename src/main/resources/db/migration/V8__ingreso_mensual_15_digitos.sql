-- El ingreso mensual admite hasta 15 dígitos enteros y 2 decimales (antes NUMERIC(15,2) = 13 enteros).
ALTER TABLE clientes ALTER COLUMN laboral_ingreso_mensual TYPE NUMERIC(17, 2);
