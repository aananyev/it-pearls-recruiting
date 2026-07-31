-- ============================================================
-- 260731-1: Маржинальность рейтов аутстафа + аудит изменений
-- HRM HuntTech (CUBA 7.3)
--
-- Что делает:
--   1. Колонки маржинальности в HUNTTECH_OUTSTAFFING_RATES:
--      MARGIN_TK / MARGIN_IE (%), NET_PROFIT_TK / NET_PROFIT_IE (руб/мес);
--   2. Функция fn_outstaffing_margin_recalc + триггер trg_orr_margin_recalc —
--      автопересчёт при изменении rate / max_salary / max_ie_salary;
--   3. Аудит-таблица HUNTTECH_OUTSTAFFING_RATES_HISTORY (INSERT/UPDATE с
--      полными снимками строки) + fn_outstaffing_rates_audit + триггеры
--      trg_orr_audit_insert / trg_orr_audit_update.
--
-- Идемпотентно: повторный накат безопасен (IF NOT EXISTS / DROP IF EXISTS /
-- CREATE OR REPLACE). Регистрацию в SYS_DB_CHANGELOG выполняет updateDb
-- автоматически — INSERT'ить в неё вручную НЕ нужно.
--
-- Константы финансовой модели (налоги 2026, аккредитованная ИТ-компания):
--   часы/мес = 164; НДС 5% без вычетов + УСН 6% = 5/105 + 0.06 от выручки;
--   взносы ТК = 15% до ЕПВБ 2 979 000/год, 7.6% сверх; травматизм 0.2%;
--   НДФЛ 13% (до гросс 200 000/мес) / 15% (свыше); отпуск ТК: выручка × 227/247.
-- При изменении законодательства: обновить функцию и выполнить
-- UPDATE HUNTTECH_OUTSTAFFING_RATES SET rate = rate;
-- ============================================================

-- 1. Колонки маржинальности (идемпотентно)
ALTER TABLE HUNTTECH_OUTSTAFFING_RATES
  ADD COLUMN IF NOT EXISTS MARGIN_TK      NUMERIC(8,2),  -- маржа ТК, % (с учётом отпуска 227/247)
  ADD COLUMN IF NOT EXISTS MARGIN_IE      NUMERIC(8,2),  -- маржа ИП, % от выручки (ставка × 164 ч)
  ADD COLUMN IF NOT EXISTS NET_PROFIT_TK  NUMERIC(19,2), -- чистая прибыль ТК, руб/мес
  ADD COLUMN IF NOT EXISTS NET_PROFIT_IE  NUMERIC(19,2); -- чистая прибыль ИП, руб/мес

COMMENT ON COLUMN HUNTTECH_OUTSTAFFING_RATES.MARGIN_TK     IS 'Маржа по ТК, % (выручка × 227/247 с учётом отпуска 20 раб. дней)';
COMMENT ON COLUMN HUNTTECH_OUTSTAFFING_RATES.MARGIN_IE     IS 'Маржа по ИП, % от выручки (ставка × 164 ч)';
COMMENT ON COLUMN HUNTTECH_OUTSTAFFING_RATES.NET_PROFIT_TK IS 'Чистая прибыль по ТК, руб/мес (после НДС 5% + УСН 6% + ФОТ с взносами 15%/7.6%)';
COMMENT ON COLUMN HUNTTECH_OUTSTAFFING_RATES.NET_PROFIT_IE IS 'Чистая прибыль по ИП, руб/мес (после НДС 5% + УСН 6% + выплаты ИП)';

-- 2. Функция пересчёта маржинальности
CREATE OR REPLACE FUNCTION fn_outstaffing_margin_recalc() RETURNS TRIGGER AS $$
DECLARE
  v_rev    NUMERIC;  -- выручка при полной отработке 164 ч
  v_tax    NUMERIC;  -- НДС 5% + УСН 6%
  v_gross  NUMERIC;  -- гросс ТК из зарплаты "на руки"
  v_cost   NUMERIC;  -- гросс + взносы + травматизм
BEGIN
  IF NEW.rate IS NULL THEN
    NEW.margin_tk := NULL; NEW.margin_ie := NULL;
    NEW.net_profit_tk := NULL; NEW.net_profit_ie := NULL;
    RETURN NEW;
  END IF;

  v_rev := NEW.rate * 164;
  v_tax := v_rev * (5.0/105.0 + 0.06);

  -- ИП: выплата = max_ie_salary, взносов нет
  IF NEW.max_ie_salary IS NULL THEN
    NEW.net_profit_ie := NULL;
    NEW.margin_ie := NULL;
  ELSE
    NEW.net_profit_ie := ROUND(v_rev - v_tax - NEW.max_ie_salary, 2);
    NEW.margin_ie     := ROUND((NEW.net_profit_ie / v_rev) * 100, 2);
  END IF;

  -- ТК: на руки = max_salary; гросс с учётом НДФЛ 13%/15%;
  -- взносы 15% до ЕПВБ 2 979 000, 7.6% сверх; травматизм 0.2%;
  -- выручка с учётом отпуска (20 раб. дней из 247)
  IF NEW.max_salary IS NULL THEN
    NEW.net_profit_tk := NULL;
    NEW.margin_tk := NULL;
  ELSE
    IF NEW.max_salary <= 174000 THEN
      v_gross := NEW.max_salary / 0.87;
    ELSE
      v_gross := (NEW.max_salary - 4000) / 0.85;
    END IF;
    v_cost := v_gross
            + (LEAST(v_gross * 12, 2979000) * 0.15) / 12
            + (GREATEST(v_gross * 12 - 2979000, 0) * 0.076) / 12
            + v_gross * 0.002;
    NEW.net_profit_tk := ROUND(v_rev * (227.0/247.0) * (1 - (5.0/105.0 + 0.06)) - v_cost, 2);
    NEW.margin_tk     := ROUND((NEW.net_profit_tk / (v_rev * 227.0/247.0)) * 100, 2);
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Триггер: пересчёт при изменении ставки или зарплатных предложений
DROP TRIGGER IF EXISTS trg_orr_margin_recalc ON HUNTTECH_OUTSTAFFING_RATES;
CREATE TRIGGER trg_orr_margin_recalc
  BEFORE INSERT OR UPDATE OF rate, max_salary, max_ie_salary
  ON HUNTTECH_OUTSTAFFING_RATES
  FOR EACH ROW EXECUTE FUNCTION fn_outstaffing_margin_recalc();

-- 4. Аудит-таблица. FK на источник НЕ ставим намеренно: история должна
--    пережить даже жёсткое удаление строки-источника.
CREATE TABLE IF NOT EXISTS HUNTTECH_OUTSTAFFING_RATES_HISTORY (
  id                  BIGSERIAL PRIMARY KEY,
  outstaffing_rate_id UUID NOT NULL,          -- id строки-источника
  action              VARCHAR(10) NOT NULL CHECK (action IN ('INSERT','UPDATE')),
  changed_at          TIMESTAMP NOT NULL DEFAULT now(),
  changed_by          VARCHAR(50),            -- created_by/updated_by из CUBA, иначе current_user
  old_rate            NUMERIC(19,2),
  new_rate            NUMERIC(19,2),
  old_min_salary      NUMERIC(19,2),
  new_min_salary      NUMERIC(19,2),
  old_max_salary      NUMERIC(19,2),
  new_max_salary      NUMERIC(19,2),
  old_max_ie_salary   NUMERIC(19,2),
  new_max_ie_salary   NUMERIC(19,2),
  old_comment         TEXT,
  new_comment         TEXT,
  old_data            JSONB,                  -- полный снимок строки ДО
  new_data            JSONB                   -- полный снимок строки ПОСЛЕ
);

CREATE INDEX IF NOT EXISTS idx_orr_history_on_rate_id
  ON HUNTTECH_OUTSTAFFING_RATES_HISTORY (outstaffing_rate_id, changed_at);

-- 5. Функция-обработчик аудита
CREATE OR REPLACE FUNCTION fn_outstaffing_rates_audit() RETURNS TRIGGER AS $$
DECLARE
  v_changed_by VARCHAR(50);
BEGIN
  IF TG_OP = 'INSERT' THEN
    v_changed_by := COALESCE(NEW.created_by, current_user);
    INSERT INTO HUNTTECH_OUTSTAFFING_RATES_HISTORY
      (outstaffing_rate_id, action, changed_by,
       new_rate, new_min_salary, new_max_salary, new_max_ie_salary, new_comment,
       new_data)
    VALUES
      (NEW.id, 'INSERT', v_changed_by,
       NEW.rate, NEW.min_salary, NEW.max_salary, NEW.max_ie_salary, NEW.comment_,
       to_jsonb(NEW));
    RETURN NEW;

  ELSIF TG_OP = 'UPDATE' THEN
    v_changed_by := COALESCE(NEW.updated_by, NEW.created_by, current_user);
    INSERT INTO HUNTTECH_OUTSTAFFING_RATES_HISTORY
      (outstaffing_rate_id, action, changed_by,
       old_rate, new_rate,
       old_min_salary, new_min_salary,
       old_max_salary, new_max_salary,
       old_max_ie_salary, new_max_ie_salary,
       old_comment, new_comment,
       old_data, new_data)
    VALUES
      (NEW.id, 'UPDATE', v_changed_by,
       OLD.rate, NEW.rate,
       OLD.min_salary, NEW.min_salary,
       OLD.max_salary, NEW.max_salary,
       OLD.max_ie_salary, NEW.max_ie_salary,
       OLD.comment_, NEW.comment_,
       to_jsonb(OLD), to_jsonb(NEW));
    RETURN NEW;
  END IF;

  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 6. Триггеры аудита: INSERT всегда, UPDATE — только если реально изменилось
--    хотя бы одно бизнес-поле (системные version/update_ts не считаем).
DROP TRIGGER IF EXISTS trg_orr_audit_insert ON HUNTTECH_OUTSTAFFING_RATES;
CREATE TRIGGER trg_orr_audit_insert
  AFTER INSERT ON HUNTTECH_OUTSTAFFING_RATES
  FOR EACH ROW EXECUTE FUNCTION fn_outstaffing_rates_audit();

DROP TRIGGER IF EXISTS trg_orr_audit_update ON HUNTTECH_OUTSTAFFING_RATES;
CREATE TRIGGER trg_orr_audit_update
  AFTER UPDATE ON HUNTTECH_OUTSTAFFING_RATES
  FOR EACH ROW
  WHEN (OLD.rate             IS DISTINCT FROM NEW.rate
     OR OLD.min_salary       IS DISTINCT FROM NEW.min_salary
     OR OLD.max_salary       IS DISTINCT FROM NEW.max_salary
     OR OLD.max_ie_salary    IS DISTINCT FROM NEW.max_ie_salary
     OR OLD.currency_id      IS DISTINCT FROM NEW.currency_id
     OR OLD.comment_         IS DISTINCT FROM NEW.comment_
     OR OLD.delete_ts        IS DISTINCT FROM NEW.delete_ts)
  EXECUTE FUNCTION fn_outstaffing_rates_audit();

-- 7. Заполнение существующих строк (rate = rate только для срабатывания
--    триггера пересчёта; аудит-триггер при неизменных значениях в историю НЕ пишет)
UPDATE HUNTTECH_OUTSTAFFING_RATES SET rate = rate;
