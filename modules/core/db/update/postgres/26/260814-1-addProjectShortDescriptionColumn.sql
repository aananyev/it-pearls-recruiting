-- ============================================================
-- 260814-1: Колонка SHORT_DESCRIPTION в HUNTTECH_PROJECT
-- HRM HuntTech (CUBA 7.3)
--
-- Что делает: добавляет «Коротко о проекте» — краткое описание сути
-- проекта (до 5 предложений), генерируется AI по кнопке «Кратко» во
-- вкладке «Описание проекта» ProjectEdit и выводится в sidebar-разделе
-- «Коротко».
--
-- Идемпотентно: повторный накат безопасен (ADD COLUMN IF NOT EXISTS).
-- Регистрацию в SYS_DB_CHANGELOG выполняет updateDb автоматически.
-- ============================================================

ALTER TABLE HUNTTECH_PROJECT
  ADD COLUMN IF NOT EXISTS SHORT_DESCRIPTION TEXT;

COMMENT ON COLUMN HUNTTECH_PROJECT.SHORT_DESCRIPTION IS
  'Краткое описание сути проекта (до 5 предложений), генерируется AI в ProjectEdit, выводится в sidebar «Коротко»';
