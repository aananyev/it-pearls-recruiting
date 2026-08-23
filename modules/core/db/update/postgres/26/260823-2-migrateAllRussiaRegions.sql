-- 260823-2-migrateAllRussiaRegions.sql
-- Массовая миграция всех 85 субъектов РФ в справочник HUNTTECH_REGION
-- Источник: ISO 3166-2:RU, гербы Wikimedia Commons
-- Применять ТОЛЬКО через Hermes-1 (прогон миграций на прод-БД)

-- Вставляем регион, если его ещё нет (по названию или ISO-коду),
-- иначе обновляем сопутствующую информацию (идемпотентно).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
DO $$
DECLARE
    v_country_id UUID := (SELECT id FROM hunttech_country WHERE country_short_name = 'RU' AND delete_ts IS NULL LIMIT 1);
    v_region_id UUID;
BEGIN
    IF v_country_id IS NULL THEN
        RAISE NOTICE 'Страна Россия (RU) не найдена — регионы не загружаются';
    ELSE
        -- Республика Адыгея
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-AD' OR region_ru_name = 'Республика Адыгея') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Адыгея', 'Республика', 'Майкоп', 'UTC+3', 'RU-AD', 'https://upload.wikimedia.org/wikipedia/commons/d/d2/Coat_of_arms_of_Adygea.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Адыгея',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Майкоп'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-AD'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/d/d2/Coat_of_arms_of_Adygea.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Алтай
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-AL' OR region_ru_name = 'Республика Алтай') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Алтай', 'Республика', 'Горно-Алтайск', 'UTC+7', 'RU-AL', 'https://upload.wikimedia.org/wikipedia/commons/c/cf/Coat_of_Arms_of_Altai_Republic.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Алтай',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Горно-Алтайск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-AL'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/cf/Coat_of_Arms_of_Altai_Republic.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Алтайский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ALT' OR region_ru_name = 'Алтайский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Алтайский край', 'Край', 'Барнаул', 'UTC+7', 'RU-ALT', 'https://upload.wikimedia.org/wikipedia/commons/c/cb/Coat_of_Arms_of_Altai_Krai_%28Latest_version%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Алтайский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Барнаул'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-ALT'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/cb/Coat_of_Arms_of_Altai_Krai_%28Latest_version%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Амурская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-AMU' OR region_ru_name = 'Амурская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Амурская область', 'Область', 'Благовещенск', 'UTC+9', 'RU-AMU', 'https://upload.wikimedia.org/wikipedia/commons/1/15/Coat_of_arms_of_Amur_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Амурская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Благовещенск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+9'),
                   iso_code = COALESCE(iso_code, 'RU-AMU'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/15/Coat_of_arms_of_Amur_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Архангельская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ARK' OR region_ru_name = 'Архангельская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Архангельская область', 'Область', 'Архангельск', 'UTC+3', 'RU-ARK', 'https://upload.wikimedia.org/wikipedia/commons/c/c9/Coat_of_Arms_of_Arkhangelsk_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Архангельская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Архангельск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-ARK'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/c9/Coat_of_Arms_of_Arkhangelsk_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Астраханская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-AST' OR region_ru_name = 'Астраханская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Астраханская область', 'Область', 'Астрахань', 'UTC+4', 'RU-AST', 'https://upload.wikimedia.org/wikipedia/commons/5/5a/Coat_of_arms_of_Astrakhan_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Астраханская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Астрахань'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+4'),
                   iso_code = COALESCE(iso_code, 'RU-AST'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/5a/Coat_of_arms_of_Astrakhan_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Башкортостан
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-BA' OR region_ru_name = 'Республика Башкортостан') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Башкортостан', 'Республика', 'Уфа', 'UTC+5', 'RU-BA', 'https://upload.wikimedia.org/wikipedia/commons/2/26/Coat_of_arms_of_Bashkortostan.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Башкортостан',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Уфа'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-BA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/2/26/Coat_of_arms_of_Bashkortostan.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Белгородская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-BEL' OR region_ru_name = 'Белгородская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Белгородская область', 'Область', 'Белгород', 'UTC+3', 'RU-BEL', 'https://upload.wikimedia.org/wikipedia/commons/6/68/Coat_of_arms_of_Belgorod_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Белгородская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Белгород'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-BEL'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/68/Coat_of_arms_of_Belgorod_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Брянская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-BRY' OR region_ru_name = 'Брянская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Брянская область', 'Область', 'Брянск', 'UTC+3', 'RU-BRY', 'https://upload.wikimedia.org/wikipedia/commons/b/bd/Coat_of_arms_of_Bryansk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Брянская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Брянск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-BRY'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/b/bd/Coat_of_arms_of_Bryansk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Бурятия
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-BU' OR region_ru_name = 'Республика Бурятия') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Бурятия', 'Республика', 'Улан-Удэ', 'UTC+8', 'RU-BU', 'https://upload.wikimedia.org/wikipedia/commons/c/c9/Coat_of_Arms_of_Buryatia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Бурятия',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Улан-Удэ'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+8'),
                   iso_code = COALESCE(iso_code, 'RU-BU'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/c9/Coat_of_Arms_of_Buryatia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Чеченская Республика
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-CE' OR region_ru_name = 'Чеченская Республика') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Чеченская Республика', 'Республика', 'Грозный', 'UTC+3', 'RU-CE', 'https://upload.wikimedia.org/wikipedia/commons/8/83/Coat_of_arms_of_Chechnya.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Чеченская Республика',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Грозный'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-CE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/8/83/Coat_of_arms_of_Chechnya.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Челябинская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-CHE' OR region_ru_name = 'Челябинская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Челябинская область', 'Область', 'Челябинск', 'UTC+5', 'RU-CHE', 'https://upload.wikimedia.org/wikipedia/commons/b/b8/Coat_of_arms_of_Chelyabinsk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Челябинская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Челябинск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-CHE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/b/b8/Coat_of_arms_of_Chelyabinsk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Чукотский автономный округ
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-CHU' OR region_ru_name = 'Чукотский автономный округ') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Чукотский автономный округ', 'Автономный округ', 'Анадырь', 'UTC+12', 'RU-CHU', 'https://upload.wikimedia.org/wikipedia/commons/7/7d/Coat_of_Arms_of_Chukotka.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Чукотский автономный округ',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Автономный округ'),
                   capital = COALESCE(NULLIF(capital, ''), 'Анадырь'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+12'),
                   iso_code = COALESCE(iso_code, 'RU-CHU'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/7d/Coat_of_Arms_of_Chukotka.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Чувашская Республика
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-CU' OR region_ru_name = 'Чувашская Республика') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Чувашская Республика', 'Республика', 'Чебоксары', 'UTC+3', 'RU-CU', 'https://upload.wikimedia.org/wikipedia/commons/3/32/Coat_of_arms_of_Chuvashia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Чувашская Республика',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Чебоксары'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-CU'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/3/32/Coat_of_arms_of_Chuvashia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Дагестан
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-DA' OR region_ru_name = 'Республика Дагестан') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Дагестан', 'Республика', 'Махачкала', 'UTC+3', 'RU-DA', 'https://upload.wikimedia.org/wikipedia/commons/e/e7/Coat_of_arms_of_Dagestan.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Дагестан',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Махачкала'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-DA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/e/e7/Coat_of_arms_of_Dagestan.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Ингушетия
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-IN' OR region_ru_name = 'Республика Ингушетия') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Ингушетия', 'Республика', 'Магас', 'UTC+3', 'RU-IN', 'https://upload.wikimedia.org/wikipedia/commons/a/a7/Coat_of_arms_of_Ingushetia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Ингушетия',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Магас'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-IN'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/a/a7/Coat_of_arms_of_Ingushetia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Иркутская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-IRK' OR region_ru_name = 'Иркутская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Иркутская область', 'Область', 'Иркутск', 'UTC+8', 'RU-IRK', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Coat_of_arms_of_Irkutsk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Иркутская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Иркутск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+8'),
                   iso_code = COALESCE(iso_code, 'RU-IRK'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Coat_of_arms_of_Irkutsk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ивановская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-IVA' OR region_ru_name = 'Ивановская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ивановская область', 'Область', 'Иваново', 'UTC+3', 'RU-IVA', 'https://upload.wikimedia.org/wikipedia/commons/a/a9/Coat_of_Arms_of_Ivanovo_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ивановская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Иваново'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-IVA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/a/a9/Coat_of_Arms_of_Ivanovo_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Камчатский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KAM' OR region_ru_name = 'Камчатский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Камчатский край', 'Край', 'Петропавловск-Камчатский', 'UTC+12', 'RU-KAM', 'https://upload.wikimedia.org/wikipedia/commons/6/66/Coat_of_arms_of_Kamchatka_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Камчатский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Петропавловск-Камчатский'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+12'),
                   iso_code = COALESCE(iso_code, 'RU-KAM'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/66/Coat_of_arms_of_Kamchatka_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Кабардино-Балкарская Республика
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KB' OR region_ru_name = 'Кабардино-Балкарская Республика') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Кабардино-Балкарская Республика', 'Республика', 'Нальчик', 'UTC+3', 'RU-KB', 'https://upload.wikimedia.org/wikipedia/commons/6/6b/Coat_of_arms_of_Kabardino-Balkaria.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Кабардино-Балкарская Республика',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Нальчик'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KB'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/6b/Coat_of_arms_of_Kabardino-Balkaria.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Карачаево-Черкесская Республика
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KC' OR region_ru_name = 'Карачаево-Черкесская Республика') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Карачаево-Черкесская Республика', 'Республика', 'Черкесск', 'UTC+3', 'RU-KC', 'https://upload.wikimedia.org/wikipedia/commons/a/a6/Coat_of_arms_of_Karachay-Cherkessia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Карачаево-Черкесская Республика',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Черкесск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KC'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/a/a6/Coat_of_arms_of_Karachay-Cherkessia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Краснодарский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KDA' OR region_ru_name = 'Краснодарский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Краснодарский край', 'Край', 'Краснодар', 'UTC+3', 'RU-KDA', 'https://upload.wikimedia.org/wikipedia/commons/7/7e/Coat_of_arms_of_Krasnodar_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Краснодарский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Краснодар'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KDA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/7e/Coat_of_arms_of_Krasnodar_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Кемеровская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KEM' OR region_ru_name = 'Кемеровская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Кемеровская область', 'Область', 'Кемерово', 'UTC+7', 'RU-KEM', 'https://upload.wikimedia.org/wikipedia/commons/1/10/Coat_of_arms_of_Kemerovo_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Кемеровская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Кемерово'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-KEM'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/10/Coat_of_arms_of_Kemerovo_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Калининградская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KGD' OR region_ru_name = 'Калининградская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Калининградская область', 'Область', 'Калининград', 'UTC+2', 'RU-KGD', 'https://upload.wikimedia.org/wikipedia/commons/2/23/Coat_of_Arms_of_Kaliningrad_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Калининградская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Калининград'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+2'),
                   iso_code = COALESCE(iso_code, 'RU-KGD'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/2/23/Coat_of_Arms_of_Kaliningrad_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Курганская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KGN' OR region_ru_name = 'Курганская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Курганская область', 'Область', 'Курган', 'UTC+5', 'RU-KGN', 'https://upload.wikimedia.org/wikipedia/commons/c/cc/Coat_of_arms_of_Kurgan_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Курганская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Курган'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-KGN'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/cc/Coat_of_arms_of_Kurgan_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Хабаровский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KHA' OR region_ru_name = 'Хабаровский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Хабаровский край', 'Край', 'Хабаровск', 'UTC+10', 'RU-KHA', 'https://upload.wikimedia.org/wikipedia/commons/3/32/Coat_of_arms_of_Khabarovsk_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Хабаровский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Хабаровск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+10'),
                   iso_code = COALESCE(iso_code, 'RU-KHA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/3/32/Coat_of_arms_of_Khabarovsk_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ханты-Мансийский автономный округ — Югра
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KHM' OR region_ru_name = 'Ханты-Мансийский автономный округ — Югра') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ханты-Мансийский автономный округ — Югра', 'Автономный округ', 'Ханты-Мансийск', 'UTC+5', 'RU-KHM', 'https://upload.wikimedia.org/wikipedia/commons/e/eb/Coat_of_arms_of_Yugra_%28Khanty-Mansia%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ханты-Мансийский автономный округ — Югра',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Автономный округ'),
                   capital = COALESCE(NULLIF(capital, ''), 'Ханты-Мансийск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-KHM'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/e/eb/Coat_of_arms_of_Yugra_%28Khanty-Mansia%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Кировская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KIR' OR region_ru_name = 'Кировская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Кировская область', 'Область', 'Киров', 'UTC+3', 'RU-KIR', 'https://upload.wikimedia.org/wikipedia/commons/6/6e/Coat_of_arms_of_Kirov_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Кировская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Киров'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KIR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/6e/Coat_of_arms_of_Kirov_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Хакасия
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KK' OR region_ru_name = 'Республика Хакасия') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Хакасия', 'Республика', 'Абакан', 'UTC+7', 'RU-KK', 'https://upload.wikimedia.org/wikipedia/commons/3/37/Coat_of_arms_of_Khakassia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Хакасия',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Абакан'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-KK'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/3/37/Coat_of_arms_of_Khakassia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Калмыкия
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KL' OR region_ru_name = 'Республика Калмыкия') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Калмыкия', 'Республика', 'Элиста', 'UTC+3', 'RU-KL', 'https://upload.wikimedia.org/wikipedia/commons/9/95/Coat_of_Arms_of_Kalmykia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Калмыкия',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Элиста'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KL'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/9/95/Coat_of_Arms_of_Kalmykia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Калужская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KLU' OR region_ru_name = 'Калужская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Калужская область', 'Область', 'Калуга', 'UTC+3', 'RU-KLU', 'https://upload.wikimedia.org/wikipedia/commons/5/58/Coat_of_arms_of_Kaluga_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Калужская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Калуга'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KLU'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/58/Coat_of_arms_of_Kaluga_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Коми
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KO' OR region_ru_name = 'Республика Коми') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Коми', 'Республика', 'Сыктывкар', 'UTC+3', 'RU-KO', 'https://upload.wikimedia.org/wikipedia/commons/f/fe/Coat_of_Arms_of_the_Komi_Republic.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Коми',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Сыктывкар'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KO'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/f/fe/Coat_of_Arms_of_the_Komi_Republic.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Костромская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KOS' OR region_ru_name = 'Костромская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Костромская область', 'Область', 'Кострома', 'UTC+3', 'RU-KOS', 'https://upload.wikimedia.org/wikipedia/commons/5/5e/Coat_of_arms_of_Kostroma_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Костромская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Кострома'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KOS'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/5e/Coat_of_arms_of_Kostroma_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Карелия
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KR' OR region_ru_name = 'Республика Карелия') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Карелия', 'Республика', 'Петрозаводск', 'UTC+3', 'RU-KR', 'https://upload.wikimedia.org/wikipedia/commons/0/0d/Coat_of_arms_of_the_Republic_of_Karelia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Карелия',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Петрозаводск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/0d/Coat_of_arms_of_the_Republic_of_Karelia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Курская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KRS' OR region_ru_name = 'Курская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Курская область', 'Область', 'Курск', 'UTC+3', 'RU-KRS', 'https://upload.wikimedia.org/wikipedia/commons/2/29/Coat_of_arms_of_Kursk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Курская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Курск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-KRS'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/2/29/Coat_of_arms_of_Kursk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Красноярский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-KYA' OR region_ru_name = 'Красноярский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Красноярский край', 'Край', 'Красноярск', 'UTC+7', 'RU-KYA', 'https://upload.wikimedia.org/wikipedia/commons/2/29/Coat_of_arms_of_Krasnoyarsk_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Красноярский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Красноярск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-KYA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/2/29/Coat_of_arms_of_Krasnoyarsk_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ленинградская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-LEN' OR region_ru_name = 'Ленинградская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ленинградская область', 'Область', 'Санкт-Петербург', 'UTC+3', 'RU-LEN', 'https://upload.wikimedia.org/wikipedia/commons/1/16/Coat_of_arms_of_Leningrad_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ленинградская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Санкт-Петербург'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-LEN'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/16/Coat_of_arms_of_Leningrad_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Липецкая область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-LIP' OR region_ru_name = 'Липецкая область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Липецкая область', 'Область', 'Липецк', 'UTC+3', 'RU-LIP', 'https://upload.wikimedia.org/wikipedia/commons/9/99/Coat_of_arms_of_Lipetsk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Липецкая область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Липецк'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-LIP'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/9/99/Coat_of_arms_of_Lipetsk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Магаданская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-MAG' OR region_ru_name = 'Магаданская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Магаданская область', 'Область', 'Магадан', 'UTC+11', 'RU-MAG', 'https://upload.wikimedia.org/wikipedia/commons/b/b0/Coat_of_Arms_of_Magadan_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Магаданская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Магадан'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+11'),
                   iso_code = COALESCE(iso_code, 'RU-MAG'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/b/b0/Coat_of_Arms_of_Magadan_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Марий Эл
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ME' OR region_ru_name = 'Республика Марий Эл') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Марий Эл', 'Республика', 'Йошкар-Ола', 'UTC+3', 'RU-ME', 'https://upload.wikimedia.org/wikipedia/commons/3/3a/Coat_of_Arms_of_Mari_El.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Марий Эл',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Йошкар-Ола'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-ME'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/3/3a/Coat_of_Arms_of_Mari_El.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Мордовия
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-MO' OR region_ru_name = 'Республика Мордовия') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Мордовия', 'Республика', 'Саранск', 'UTC+3', 'RU-MO', 'https://upload.wikimedia.org/wikipedia/commons/6/60/Coat_of_arms_of_Mordovia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Мордовия',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Саранск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-MO'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/60/Coat_of_arms_of_Mordovia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Московская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-MOS' OR region_ru_name = 'Московская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Московская область', 'Область', 'Красногорск', 'UTC+3', 'RU-MOS', 'https://upload.wikimedia.org/wikipedia/commons/9/9c/Coat_of_arms_of_Moscow_Oblast_%28large%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Московская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Красногорск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-MOS'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/9/9c/Coat_of_arms_of_Moscow_Oblast_%28large%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Москва
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-MOW' OR region_ru_name = 'Москва') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Москва', 'Город федерального значения', 'Москва', 'UTC+3', 'RU-MOW', 'https://upload.wikimedia.org/wikipedia/commons/1/17/Coat_of_arms_of_Moscow.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Москва',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Город федерального значения'),
                   capital = COALESCE(NULLIF(capital, ''), 'Москва'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-MOW'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/17/Coat_of_arms_of_Moscow.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Мурманская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-MUR' OR region_ru_name = 'Мурманская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Мурманская область', 'Область', 'Мурманск', 'UTC+3', 'RU-MUR', 'https://upload.wikimedia.org/wikipedia/commons/3/3b/Coat_of_arms_of_Murmansk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Мурманская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Мурманск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-MUR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/3/3b/Coat_of_arms_of_Murmansk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ненецкий автономный округ
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-NEN' OR region_ru_name = 'Ненецкий автономный округ') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ненецкий автономный округ', 'Автономный округ', 'Нарьян-Мар', 'UTC+3', 'RU-NEN', 'https://upload.wikimedia.org/wikipedia/commons/4/47/Coat_of_arms_of_Nenets_Autonomous_Okrug.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ненецкий автономный округ',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Автономный округ'),
                   capital = COALESCE(NULLIF(capital, ''), 'Нарьян-Мар'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-NEN'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/4/47/Coat_of_arms_of_Nenets_Autonomous_Okrug.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Новгородская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-NGR' OR region_ru_name = 'Новгородская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Новгородская область', 'Область', 'Великий Новгород', 'UTC+3', 'RU-NGR', 'https://upload.wikimedia.org/wikipedia/commons/1/1a/Coat_of_arms_of_Novgorod_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Новгородская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Великий Новгород'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-NGR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/1a/Coat_of_arms_of_Novgorod_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Нижегородская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-NIZ' OR region_ru_name = 'Нижегородская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Нижегородская область', 'Область', 'Нижний Новгород', 'UTC+3', 'RU-NIZ', 'https://upload.wikimedia.org/wikipedia/commons/7/73/Coat_of_arms_of_Nizhny_Novgorod_Region.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Нижегородская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Нижний Новгород'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-NIZ'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/73/Coat_of_arms_of_Nizhny_Novgorod_Region.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Новосибирская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-NVS' OR region_ru_name = 'Новосибирская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Новосибирская область', 'Область', 'Новосибирск', 'UTC+7', 'RU-NVS', 'https://upload.wikimedia.org/wikipedia/commons/7/73/Coat_of_arms_of_Novosibirsk_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Новосибирская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Новосибирск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-NVS'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/73/Coat_of_arms_of_Novosibirsk_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Омская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-OMS' OR region_ru_name = 'Омская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Омская область', 'Область', 'Омск', 'UTC+6', 'RU-OMS', 'https://upload.wikimedia.org/wikipedia/commons/0/06/Coat_of_arms_of_Omsk_Oblast_%282003-2020%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Омская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Омск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+6'),
                   iso_code = COALESCE(iso_code, 'RU-OMS'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/06/Coat_of_arms_of_Omsk_Oblast_%282003-2020%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Оренбургская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ORE' OR region_ru_name = 'Оренбургская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Оренбургская область', 'Область', 'Оренбург', 'UTC+5', 'RU-ORE', 'https://upload.wikimedia.org/wikipedia/commons/b/b0/Coat_of_arms_of_Orenburg_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Оренбургская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Оренбург'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-ORE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/b/b0/Coat_of_arms_of_Orenburg_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Орловская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ORL' OR region_ru_name = 'Орловская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Орловская область', 'Область', 'Орёл', 'UTC+3', 'RU-ORL', 'https://upload.wikimedia.org/wikipedia/commons/c/cb/Coat_of_arms_of_Oryol_Oblast_%28large%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Орловская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Орёл'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-ORL'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/cb/Coat_of_arms_of_Oryol_Oblast_%28large%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Пермский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-PER' OR region_ru_name = 'Пермский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Пермский край', 'Край', 'Пермь', 'UTC+5', 'RU-PER', 'https://upload.wikimedia.org/wikipedia/commons/1/1b/Coat_of_Arms_of_Perm_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Пермский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Пермь'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-PER'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/1b/Coat_of_Arms_of_Perm_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Пензенская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-PNZ' OR region_ru_name = 'Пензенская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Пензенская область', 'Область', 'Пенза', 'UTC+3', 'RU-PNZ', 'https://upload.wikimedia.org/wikipedia/commons/1/10/Coat_of_arms_of_Penza_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Пензенская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Пенза'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-PNZ'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/10/Coat_of_arms_of_Penza_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Приморский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-PRI' OR region_ru_name = 'Приморский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Приморский край', 'Край', 'Владивосток', 'UTC+10', 'RU-PRI', 'https://upload.wikimedia.org/wikipedia/commons/6/65/Coat_of_arms_of_Primorsky_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Приморский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Владивосток'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+10'),
                   iso_code = COALESCE(iso_code, 'RU-PRI'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/65/Coat_of_arms_of_Primorsky_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Псковская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-PSK' OR region_ru_name = 'Псковская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Псковская область', 'Область', 'Псков', 'UTC+3', 'RU-PSK', 'https://upload.wikimedia.org/wikipedia/commons/0/05/Coat_of_arms_of_Pskov_Oblast_%282018%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Псковская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Псков'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-PSK'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/05/Coat_of_arms_of_Pskov_Oblast_%282018%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ростовская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ROS' OR region_ru_name = 'Ростовская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ростовская область', 'Область', 'Ростов-на-Дону', 'UTC+3', 'RU-ROS', 'https://upload.wikimedia.org/wikipedia/commons/c/cf/Coat_of_arms_of_Rostov_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ростовская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Ростов-на-Дону'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-ROS'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/cf/Coat_of_arms_of_Rostov_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Рязанская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-RYA' OR region_ru_name = 'Рязанская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Рязанская область', 'Область', 'Рязань', 'UTC+3', 'RU-RYA', 'https://upload.wikimedia.org/wikipedia/commons/0/02/Coat_of_arms_of_Ryazan_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Рязанская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Рязань'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-RYA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/02/Coat_of_arms_of_Ryazan_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Саха (Якутия)
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SA' OR region_ru_name = 'Республика Саха (Якутия)') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Саха (Якутия)', 'Республика', 'Якутск', 'UTC+9', 'RU-SA', 'https://upload.wikimedia.org/wikipedia/commons/b/b8/Coat_of_Arms_of_Sakha_%28Yakutia%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Саха (Якутия)',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Якутск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+9'),
                   iso_code = COALESCE(iso_code, 'RU-SA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/b/b8/Coat_of_Arms_of_Sakha_%28Yakutia%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Сахалинская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SAK' OR region_ru_name = 'Сахалинская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Сахалинская область', 'Область', 'Южно-Сахалинск', 'UTC+11', 'RU-SAK', 'https://upload.wikimedia.org/wikipedia/commons/0/0f/Sakhalin_Oblast_Coat_of_Arms.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Сахалинская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Южно-Сахалинск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+11'),
                   iso_code = COALESCE(iso_code, 'RU-SAK'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/0f/Sakhalin_Oblast_Coat_of_Arms.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Самарская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SAM' OR region_ru_name = 'Самарская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Самарская область', 'Область', 'Самара', 'UTC+4', 'RU-SAM', 'https://upload.wikimedia.org/wikipedia/commons/5/53/Coat_of_arms_of_Samara_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Самарская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Самара'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+4'),
                   iso_code = COALESCE(iso_code, 'RU-SAM'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/53/Coat_of_arms_of_Samara_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Саратовская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SAR' OR region_ru_name = 'Саратовская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Саратовская область', 'Область', 'Саратов', 'UTC+4', 'RU-SAR', 'https://upload.wikimedia.org/wikipedia/commons/5/53/Coat_of_Arms_of_Saratov_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Саратовская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Саратов'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+4'),
                   iso_code = COALESCE(iso_code, 'RU-SAR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/53/Coat_of_Arms_of_Saratov_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Северная Осетия — Алания
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SE' OR region_ru_name = 'Республика Северная Осетия — Алания') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Северная Осетия — Алания', 'Республика', 'Владикавказ', 'UTC+3', 'RU-SE', 'https://upload.wikimedia.org/wikipedia/commons/1/1f/Emblem_of_North_Ossetia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Северная Осетия — Алания',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Владикавказ'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-SE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/1/1f/Emblem_of_North_Ossetia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Смоленская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SMO' OR region_ru_name = 'Смоленская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Смоленская область', 'Область', 'Смоленск', 'UTC+3', 'RU-SMO', 'https://upload.wikimedia.org/wikipedia/commons/0/03/Coat_of_arms_of_Smolensk_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Смоленская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Смоленск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-SMO'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/03/Coat_of_arms_of_Smolensk_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Санкт-Петербург
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SPE' OR region_ru_name = 'Санкт-Петербург') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Санкт-Петербург', 'Город федерального значения', 'Санкт-Петербург', 'UTC+3', 'RU-SPE', 'https://upload.wikimedia.org/wikipedia/commons/c/cd/Coat_of_arms_of_Saint_Petersburg_%282003%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Санкт-Петербург',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Город федерального значения'),
                   capital = COALESCE(NULLIF(capital, ''), 'Санкт-Петербург'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-SPE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/cd/Coat_of_arms_of_Saint_Petersburg_%282003%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ставропольский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-STA' OR region_ru_name = 'Ставропольский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ставропольский край', 'Край', 'Ставрополь', 'UTC+3', 'RU-STA', 'https://upload.wikimedia.org/wikipedia/commons/5/5d/Coat_of_arms_of_Stavropol_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ставропольский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Ставрополь'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-STA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/5d/Coat_of_arms_of_Stavropol_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Свердловская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SVE' OR region_ru_name = 'Свердловская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Свердловская область', 'Область', 'Екатеринбург', 'UTC+5', 'RU-SVE', 'https://upload.wikimedia.org/wikipedia/commons/4/40/Coat_of_Arms_of_Sverdlovsk_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Свердловская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Екатеринбург'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-SVE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/4/40/Coat_of_Arms_of_Sverdlovsk_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Татарстан
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TA' OR region_ru_name = 'Республика Татарстан') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Татарстан', 'Республика', 'Казань', 'UTC+3', 'RU-TA', 'https://upload.wikimedia.org/wikipedia/commons/9/90/Coat_of_arms_of_Tatarstan.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Татарстан',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Казань'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-TA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/9/90/Coat_of_arms_of_Tatarstan.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Тамбовская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TAM' OR region_ru_name = 'Тамбовская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Тамбовская область', 'Область', 'Тамбов', 'UTC+3', 'RU-TAM', 'https://upload.wikimedia.org/wikipedia/commons/a/ac/Coat_of_arms_of_Tambov_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Тамбовская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Тамбов'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-TAM'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/a/ac/Coat_of_arms_of_Tambov_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Томская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TOM' OR region_ru_name = 'Томская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Томская область', 'Область', 'Томск', 'UTC+7', 'RU-TOM', 'https://upload.wikimedia.org/wikipedia/commons/8/89/Coat_of_arms_of_Tomsk_Oblast%2C_Russia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Томская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Томск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-TOM'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/8/89/Coat_of_arms_of_Tomsk_Oblast%2C_Russia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Тульская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TUL' OR region_ru_name = 'Тульская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Тульская область', 'Область', 'Тула', 'UTC+3', 'RU-TUL', 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Coat_of_Arms_of_Tula_oblast_%282000%29.png', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Тульская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Тула'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-TUL'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Coat_of_Arms_of_Tula_oblast_%282000%29.png'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Тверская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TVE' OR region_ru_name = 'Тверская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Тверская область', 'Область', 'Тверь', 'UTC+3', 'RU-TVE', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Coat_of_arms_of_Tver_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Тверская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Тверь'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-TVE'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/70/Coat_of_arms_of_Tver_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Тыва
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TY' OR region_ru_name = 'Республика Тыва') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Тыва', 'Республика', 'Кызыл', 'UTC+7', 'RU-TY', 'https://upload.wikimedia.org/wikipedia/commons/c/c3/Coat_of_arms_of_Tuva.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Тыва',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Кызыл'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+7'),
                   iso_code = COALESCE(iso_code, 'RU-TY'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/c3/Coat_of_arms_of_Tuva.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Тюменская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-TYU' OR region_ru_name = 'Тюменская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Тюменская область', 'Область', 'Тюмень', 'UTC+5', 'RU-TYU', 'https://upload.wikimedia.org/wikipedia/commons/2/2a/Coat_of_Arms_of_Tyumen_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Тюменская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Тюмень'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-TYU'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/2/2a/Coat_of_Arms_of_Tyumen_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Удмуртская Республика
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-UD' OR region_ru_name = 'Удмуртская Республика') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Удмуртская Республика', 'Республика', 'Ижевск', 'UTC+4', 'RU-UD', 'https://upload.wikimedia.org/wikipedia/commons/0/0a/Coat_of_arms_of_Udmurtia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Удмуртская Республика',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Ижевск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+4'),
                   iso_code = COALESCE(iso_code, 'RU-UD'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/0/0a/Coat_of_arms_of_Udmurtia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ульяновская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ULY' OR region_ru_name = 'Ульяновская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ульяновская область', 'Область', 'Ульяновск', 'UTC+4', 'RU-ULY', 'https://upload.wikimedia.org/wikipedia/commons/e/ee/%D0%93%D0%B5%D1%80%D0%B1_%D0%A3%D0%BB%D1%8C%D1%8F%D0%BD%D0%BE%D0%B2%D1%81%D0%BA%D0%BE%D0%B9_%D0%BE%D0%B1%D0%BB%D0%B0%D1%81%D1%82%D0%B8_%282013%29.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ульяновская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Ульяновск'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+4'),
                   iso_code = COALESCE(iso_code, 'RU-ULY'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/e/ee/%D0%93%D0%B5%D1%80%D0%B1_%D0%A3%D0%BB%D1%8C%D1%8F%D0%BD%D0%BE%D0%B2%D1%81%D0%BA%D0%BE%D0%B9_%D0%BE%D0%B1%D0%BB%D0%B0%D1%81%D1%82%D0%B8_%282013%29.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Волгоградская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-VGG' OR region_ru_name = 'Волгоградская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Волгоградская область', 'Область', 'Волгоград', 'UTC+3', 'RU-VGG', 'https://upload.wikimedia.org/wikipedia/commons/5/54/Coat_of_Arms_of_Volgograd_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Волгоградская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Волгоград'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-VGG'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/5/54/Coat_of_Arms_of_Volgograd_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Владимирская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-VLA' OR region_ru_name = 'Владимирская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Владимирская область', 'Область', 'Владимир', 'UTC+3', 'RU-VLA', 'https://upload.wikimedia.org/wikipedia/commons/e/ef/Coat_of_arms_of_Vladimir_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Владимирская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Владимир'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-VLA'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/e/ef/Coat_of_arms_of_Vladimir_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Вологодская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-VLG' OR region_ru_name = 'Вологодская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Вологодская область', 'Область', 'Вологда', 'UTC+3', 'RU-VLG', 'https://upload.wikimedia.org/wikipedia/commons/7/7a/Coat_of_arms_of_Vologda_oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Вологодская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Вологда'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-VLG'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/7/7a/Coat_of_arms_of_Vologda_oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Воронежская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-VOR' OR region_ru_name = 'Воронежская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Воронежская область', 'Область', 'Воронеж', 'UTC+3', 'RU-VOR', 'https://upload.wikimedia.org/wikipedia/commons/2/2a/Coat_of_arms_of_Voronezh_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Воронежская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Воронеж'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-VOR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/2/2a/Coat_of_arms_of_Voronezh_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ямало-Ненецкий автономный округ
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-YAN' OR region_ru_name = 'Ямало-Ненецкий автономный округ') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ямало-Ненецкий автономный округ', 'Автономный округ', 'Салехард', 'UTC+5', 'RU-YAN', 'https://upload.wikimedia.org/wikipedia/commons/d/d0/Coat_of_Arms_of_Yamal_Nenetsia.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ямало-Ненецкий автономный округ',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Автономный округ'),
                   capital = COALESCE(NULLIF(capital, ''), 'Салехард'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+5'),
                   iso_code = COALESCE(iso_code, 'RU-YAN'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/d/d0/Coat_of_Arms_of_Yamal_Nenetsia.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Ярославская область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-YAR' OR region_ru_name = 'Ярославская область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Ярославская область', 'Область', 'Ярославль', 'UTC+3', 'RU-YAR', 'https://upload.wikimedia.org/wikipedia/commons/4/48/Coat_of_arms_of_Yaroslavl_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Ярославская область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Ярославль'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-YAR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/4/48/Coat_of_arms_of_Yaroslavl_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Еврейская автономная область
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-YEV' OR region_ru_name = 'Еврейская автономная область') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Еврейская автономная область', 'Автономная область', 'Биробиджан', 'UTC+10', 'RU-YEV', 'https://upload.wikimedia.org/wikipedia/commons/c/ce/Coat_of_arms_of_Jewish_Autonomous_Oblast.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Еврейская автономная область',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Автономная область'),
                   capital = COALESCE(NULLIF(capital, ''), 'Биробиджан'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+10'),
                   iso_code = COALESCE(iso_code, 'RU-YEV'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/ce/Coat_of_arms_of_Jewish_Autonomous_Oblast.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Забайкальский край
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-ZAB' OR region_ru_name = 'Забайкальский край') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Забайкальский край', 'Край', 'Чита', 'UTC+9', 'RU-ZAB', 'https://upload.wikimedia.org/wikipedia/commons/6/6a/Coat_of_arms_of_Zabaykalsky_Krai.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Забайкальский край',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Край'),
                   capital = COALESCE(NULLIF(capital, ''), 'Чита'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+9'),
                   iso_code = COALESCE(iso_code, 'RU-ZAB'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/6/6a/Coat_of_arms_of_Zabaykalsky_Krai.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Республика Крым
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-CR' OR region_ru_name = 'Республика Крым') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Республика Крым', 'Республика', 'Симферополь', 'UTC+3', 'RU-CR', 'https://upload.wikimedia.org/wikipedia/commons/c/c8/Emblem_of_Crimea.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Республика Крым',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Республика'),
                   capital = COALESCE(NULLIF(capital, ''), 'Симферополь'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-CR'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/c/c8/Emblem_of_Crimea.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
        -- Севастополь
        SELECT id INTO v_region_id FROM hunttech_region WHERE delete_ts IS NULL AND (iso_code = 'RU-SEV' OR region_ru_name = 'Севастополь') LIMIT 1;
        IF v_region_id IS NULL THEN
            INSERT INTO hunttech_region (id, version, create_ts, region_ru_name, region_type, capital, time_zone, iso_code, emblem_url, region_country_id, delete_ts)
            VALUES (gen_random_uuid(), 0, now(), 'Севастополь', 'Город федерального значения', 'Севастополь', 'UTC+3', 'RU-SEV', 'https://upload.wikimedia.org/wikipedia/commons/a/ab/COA_of_Sevastopol.svg', v_country_id, NULL);
        ELSE
            UPDATE hunttech_region
               SET region_ru_name = 'Севастополь',
                   region_type = COALESCE(NULLIF(region_type, ''), 'Город федерального значения'),
                   capital = COALESCE(NULLIF(capital, ''), 'Севастополь'),
                   time_zone = COALESCE(NULLIF(time_zone, ''), 'UTC+3'),
                   iso_code = COALESCE(iso_code, 'RU-SEV'),
                   emblem_url = COALESCE(NULLIF(emblem_url, ''), 'https://upload.wikimedia.org/wikipedia/commons/a/ab/COA_of_Sevastopol.svg'),
                   region_country_id = COALESCE(region_country_id, v_country_id),
                   version = version + 1
             WHERE id = v_region_id;
        END IF;
    END IF;
END $$;

-- Итоговый отчёт
SELECT count(*) AS russia_regions_total,
       count(iso_code) AS with_iso,
       count(emblem_url) AS with_emblem_url
FROM hunttech_region
WHERE delete_ts IS NULL AND region_country_id = (SELECT id FROM hunttech_country WHERE country_short_name='RU' AND delete_ts IS NULL);

-- Удаление старых записей-дублей с опечатками/краткими именами,
-- для которых в справочнике есть корректная версия с ISO-кодом
UPDATE hunttech_region SET delete_ts = now(), version = version + 1
WHERE delete_ts IS NULL
  AND region_country_id = (SELECT id FROM hunttech_country WHERE country_short_name='RU' AND delete_ts IS NULL)
  AND iso_code IS NULL
  AND region_ru_name IN (
    'Архангельская облась', 'Башкортостан', 'Белгородская обоасть', 'Крым',
    'Оренбуржская область', 'Саратовская обл', 'Северная Осетия', 'Удмуртия',
    'Хакасия', 'Хантымансийский автономный округ', 'Чечернская республика', 'Чувашская республика'
  );