-- 260823-3-updateCountriesFullInfo.sql
-- Массовое обогащение справочника HUNTTECH_COUNTRY: английские названия, ISO-коды (Alpha-2/Alpha-3/числовой),
-- валюта, столица, телефонный код, URL и BLOB-изображение флага (герб/символ страны).
-- Источник: dr5hn/countries-states-cities-database (ISO 3166, ODbL v1.0) + flagcdn.com (флаги).
-- Исправлены ошибки: Германия GE->DE, Китай CH->CN, Италия phone 39051->39, Израиль IZ->IL.
-- Применять ТОЛЬКО через Hermes-1 (прогон миграций на прод-БД). Идемпотентно: обновление по country_ru_name.

-- Обновляем сопутствующую информацию по каждому реальному наименованию страны (включая дубли-синонимы).
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- Австралия (AU)
    UPDATE hunttech_country SET
        country_eng_name = 'Australia',
        country_short_name = 'AU',
        alpha3_code = 'AUS',
        numeric_code = '036',
        currency_code = 'AUD',
        capital = 'Канберра',
        phone_code = 61,
        flag_url = 'https://flagcdn.com/w320/au.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Австралия' AND delete_ts IS NULL;
    -- Австрия (AT)
    UPDATE hunttech_country SET
        country_eng_name = 'Austria',
        country_short_name = 'AT',
        alpha3_code = 'AUT',
        numeric_code = '040',
        currency_code = 'EUR',
        capital = 'Вена',
        phone_code = 43,
        flag_url = 'https://flagcdn.com/w320/at.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAQMAAAAcgrutAAAABlBMVEXIEC7///94b0yLAAAAK0lEQVRo3u3KoQEAAAgDoP3/tJ6gYREyCQAA0DJPoiiKoiiKoiieEQAAaFnRGw2OGe14BQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Австрия' AND delete_ts IS NULL;
    -- Азербайджан (AZ)
    UPDATE hunttech_country SET
        country_eng_name = 'Azerbaijan',
        country_short_name = 'AZ',
        alpha3_code = 'AZE',
        numeric_code = '031',
        currency_code = 'AZN',
        capital = 'Баку',
        phone_code = 994,
        flag_url = 'https://flagcdn.com/w320/az.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAKlBMVEXvM0AAteJQni////+6VjqgXnb3mZ/wQ0/81Nf0d3/+7/DyWmT6u7/vMkAIcMTzAAACU0lEQVR42u3Zv4oTURQG8K/aOGan8BV8hQ/CbLKlr2AfSCHLNgMLERUhMKAgWwwsLKuWgVGxSacrFsI2YjWVSxzmXSzuGJOZqWzygd+pklv9mPvvnHNxTzxgoIEGGmiggQYaaKCBBhpooIEGGmiggQYaaKCBBhpooIEGGmiggQYauA/gQ/GAQyYuv796f34iy5vfkCR5/EjUx00oCuP58i9wLCgcZsH24m16fZklqZqvfhOWX14iLjG4+qgGjIIvrZv/n3Mx4EXwofyzJF+LrUCS5LetkduVFPApSY52hh4r+QYZSU53x5SABySZtPZ1CkylZvhld/yDCnBGkp1dUUbHKkuwZ4aHAA6Zhh8Sp/Rkd2xdID5jjvW5APBO+xAEUGdFOuMiyiYCicxZ95ABLlhkHC3b8P3dc+09crdJvVYimzhpf9afwZco7OIbkp0TpQwJ7JFCMp11LmIAVchgJwpfsA84bzJsFgKLcNmZysHVVoFyKg5MTj3F/wKMh0KbpPeYgc4xgxnJcW8SK3JQ9151v4SuOvlk4ZAkF+2dUzyfcbFWSLd6E9aoSVgjhYS1L+Wv4pDy15VM0ZT3Fk1xKVN2vhMuO/sKdygV7nXWLZtulVofYY6PdHszqDrtt2olBQy33XZnWqyBiSckydFG+DUXA+JL06QO//Sa6E0Pk+OTT+mz6x96zxDAwfZDzhSCIf4UBv3HRGyeY0tZYF0DcY3/Lx6IB+6Lh4EGGmiggQYaaKCBBhpooIEGGmiggQYaaKCBBhpooIEGGmiggQYaaOA+4jeGTLHKXKvABAAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Азербайджан' AND delete_ts IS NULL;
    -- Арабские Эмираты (AE)
    UPDATE hunttech_country SET
        country_eng_name = 'United Arab Emirates',
        country_short_name = 'AE',
        alpha3_code = 'ARE',
        numeric_code = '784',
        currency_code = 'AED',
        capital = 'Абу-Даби',
        phone_code = 971,
        flag_url = 'https://flagcdn.com/w320/ae.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAElBMVEUAhD0AAADIEC7///+r1r+qqqoDfhjfAAAAkklEQVR42u3OQQ0AIAwEsFnAAhawgAX8W8HCfkdCq6A1mypFUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUPDl4A5pB1eIoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCg4N/BE9IOjhBBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBwYQLGzUbx1aa+HUAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Арабские Эмираты' AND delete_ts IS NULL;
    -- Армения (AM)
    UPDATE hunttech_country SET
        country_eng_name = 'Armenia',
        country_short_name = 'AM',
        alpha3_code = 'ARM',
        numeric_code = '051',
        currency_code = 'AMD',
        capital = 'Ереван',
        phone_code = 374,
        flag_url = 'https://flagcdn.com/w320/am.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAD1BMVEUAM6DZABLyqAA3Jnw9UHgWioGBAAAAcklEQVR42u3OwQAAMBADsCpMYQqnMH+mYVwfCUFyykVQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQcCM45QIAAAAAAAAAsOGVyy0nKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKLjhA7/C5BvaXWXrAAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Армения' AND delete_ts IS NULL;
    -- Беларусь (BY)
    UPDATE hunttech_country SET
        country_eng_name = 'Belarus',
        country_short_name = 'BY',
        alpha3_code = 'BLR',
        numeric_code = '112',
        currency_code = 'BYN',
        capital = 'Минск',
        phone_code = 375,
        flag_url = 'https://flagcdn.com/w320/by.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Беларусь' AND delete_ts IS NULL;
    -- Белоруссия (BY)
    UPDATE hunttech_country SET
        country_eng_name = 'Belarus',
        country_short_name = 'BY',
        alpha3_code = 'BLR',
        numeric_code = '112',
        currency_code = 'BYN',
        capital = 'Минск',
        phone_code = 375,
        flag_url = 'https://flagcdn.com/w320/by.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Белоруссия' AND delete_ts IS NULL;
    -- Бельгия (BE)
    UPDATE hunttech_country SET
        country_eng_name = 'Belgium',
        country_short_name = 'BE',
        alpha3_code = 'BEL',
        numeric_code = '056',
        currency_code = 'EUR',
        capital = 'Брюссель',
        phone_code = 32,
        flag_url = 'https://flagcdn.com/w320/be.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAAEVBAMAAABj/SHdAAAAD1BMVEXvM0AAAAD92iVVSgzzazbqA7XGAAAA0ElEQVR42u3OMREAMAgEMCxggauC+heHh2dNFKQ68SfwKiIoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCh4CC64a6WlMgVhXgAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Бельгия' AND delete_ts IS NULL;
    -- Болгария (BG)
    UPDATE hunttech_country SET
        country_eng_name = 'Bulgaria',
        country_short_name = 'BG',
        alpha3_code = 'BGR',
        numeric_code = '100',
        currency_code = 'EUR',
        capital = 'София',
        phone_code = 359,
        flag_url = 'https://flagcdn.com/w320/bg.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADAAgMAAAAIOVJVAAAACVBMVEXWJhIAlm7///+uJ0PXAAAASUlEQVRo3u3MMREAAAgEIEta0pQmcHP6gwDUPCuhUCgUCoVCoVAoFAqFQqFQGBP2M6FQKBQKhUKhUCgUCoVCoVCYEwIAAAAAHBb2y+0e70Tw6AAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Болгария' AND delete_ts IS NULL;
    -- Бразилия (BR)
    UPDATE hunttech_country SET
        country_eng_name = 'Brazil',
        country_short_name = 'BR',
        alpha3_code = 'BRA',
        numeric_code = '076',
        currency_code = 'BRL',
        capital = 'Бразилиа',
        phone_code = 55,
        flag_url = 'https://flagcdn.com/w320/br.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Бразилия' AND delete_ts IS NULL;
    -- Великобритания (GB)
    UPDATE hunttech_country SET
        country_eng_name = 'United Kingdom',
        country_short_name = 'GB',
        alpha3_code = 'GBR',
        numeric_code = '826',
        currency_code = 'GBP',
        capital = 'Лондон',
        phone_code = 44,
        flag_url = 'https://flagcdn.com/w320/gb.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAKlBMVEX////IEC4BIWnAyNrXTmTxw8uqtc1BWY/zytH21NomQX8tR4O1v9T48fRYqhMkAAADZUlEQVR42u3av04VQRQG8O9+0mhH4QvwAhaYYGOlJGBsSGy0MphQIB0tdrR0aMEDWGnolIgmJhYGpTMxaqGdjYWtpcW9cJdl5+/ZnTMJ55Tnzuz+MrOTPbl7RouHaMX6AZJi5gcAzLayfwFg9X3apbYfAMDuk2nmOo8X26OeLUEpOnyHRD3CLh+IaoSdPhC1CLt9ICoR7nb7xsAKhLt3un0ToLrQ6TsBKgv3nL5ToKpw77bTNwUqCn2+BlBN6PWBt7SFfh/4Qlk49m27fCB0hRPfjssHQlW4EvKB0BSu7IR841OsJYzwgZt6whjfAvfVhFG+P4SWMM4HQkkY6QOhI4z1jU9xeeHYdy/smxQLpYUT37uw76SaKStM8J2WWyWFKb5pPVhOmORrFKylhGm+ZkVdRpjoawKLCFN9Z4AFhMm+s8DBhem+FnBgYYavDYwTLpfznQNGCZ8uF/OdBw4mzPN1AAcSZvq6gIMIc32dwAGE2b5uYO/CfJ8D2LNQ4HMBexVKfE5gj0KRzw3sTSjzeYA9CYU+H7AXodTnBfYgFPtwadb367fVjwC+3v11mvl982drzOf7nwDgciv9DwD4UurDaE6+RQAcH7Qh9vm3OHKX/SHzBYFiodAXBgqFUh9G8+GbXPkAAN+3ppmlR5HPoH/Sl4fhm4/6+kPIfUhkQVQeBjSgAQ140YGjWVtBAxrQgAY0oAENaEADGtCABjSgAQ1oQAMa8KIC7UuTAQ1oQAMaUBYz0U0VzQg0WHTH+UlRTRVzwSErO+1Ms9fkxuvoV11z2sIBENWWwp58MdHsvTlaAoCjq3Jgf748IQv6soSU+zbc0+e9wsUYIcW+x2/c869t+oTHMUKKfd5d3hcLOaivByGH9cmFHNgnFnJon1TIwX1CIYf3yYQs4BMJWcInEbKITyBkGV++kIV82UKW8uUKmeFbz/JlCpnhOwDKCVnQlyVkSV+OkEV9GUKW9aULWdiXLGRpX6qQxX2JQpb3pQmp4EsSUsOXIqSKL0FIHV+8kEq+aCG1fLFCqvkihdTzxQmp6AP23waF1PQBa8E1pKovYg2p6wPWAkIq+4JCavtCQqr7AkLq+/xCVuAD1l45hazBB2w4hazC5xGyDh+w8bxbyEp8wFa3kLX4XEJW43MIWY+vW/gfIFP32aSBt8UAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Великобритания' AND delete_ts IS NULL;
    -- Венгрия (HU)
    UPDATE hunttech_country SET
        country_eng_name = 'Hungary',
        country_short_name = 'HU',
        alpha3_code = 'HUN',
        numeric_code = '348',
        currency_code = 'HUF',
        capital = 'Будапешт',
        phone_code = 36,
        flag_url = 'https://flagcdn.com/w320/hu.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAD1BMVEVHcFDOKTn////zyc3R29PS+FOWAAAAc0lEQVR42u3OgRAAIAAEsFdIIYUU8mcKIID+uo1gGeUiKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKPgiuMpllhMUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT8M7jLBQAAAAAAAACA2wFkYM7bOV+RbAAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Венгрия' AND delete_ts IS NULL;
    -- Вьетнам (VN)
    UPDATE hunttech_country SET
        country_eng_name = 'Vietnam',
        country_short_name = 'VN',
        alpha3_code = 'VNM',
        numeric_code = '704',
        currency_code = 'VND',
        capital = 'Ханой',
        phone_code = 84,
        flag_url = 'https://flagcdn.com/w320/vn.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAG1BMVEX/AAD/6wD/OgD/oAD/wgD/GQD/eQD/2wD/WgAVG+ycAAAC2klEQVR42u2avXLaQBhFMcJCJWsIuBQZT2itwn3wDH2Y+AGkNwBXKUka57ETQjAC9J/Mfqe4p6a4gzhw9y69nhBCCCGEEEIIIYQQQgghhBCiAdEOHjB4hAccbuAB53fwgMkYHnA7Y+frOxezJXaOrfHQObbGc+fYGmfOTdkSOzeDS8zWONwHTMEBB/uAn9kSszXO9gHJGi/3ASdwiZ37wpaYrPHgEJCr8foQ8ANbYrLGy0NArMa3zrE1Do8BU7bEXI3Xx4BUjb8fA96zJcZq/C4xVePoFHBHPbQfeWRL7NyILTFV4+QUkLnBuRxwiZkaB/mARI1v8gFHbImZGif5gESNt/mAlhtc/2Mhn9wZP4pfFXttVe2ZeKk5YfeAng4ri675nk2EbY4/taNtl3wzj79/L10CrgxWmDb4XWz6rR/yLPb7dd36IT/5/kHJyA/4z0Nu9YMyib0HPC+odZgU2Lfm+b7ZTB1LVkf4h9aQ9oxo2Bpe7fasRq1hbLh1hegHvOcBVAI7VkPr811dNfRZAju1hlXPnIzVEdpVQ98lsJivoBLYYDYiTkhl1dCqIzR+CzkbXML8jj51hrLPIOURR2UBd5CAAeokUsBNWcARW2KOxgm1ah0p/6mDS0zROIANClcMywNu2BJT/s6acE9MB6oaNaINVp1JYrbEDI3PJX5d4DSenw9F56vhHU3i9GJQmsIkfr4clGYsicfX76m9xuH1UJQflFKSxKuCQWkDknhaNCjZa5wVDUWnQWnKkfipcFCaYCS+L3ljY4jEl7eF74OStcaDslYQQP6VPi+9LXxj/Cs9K10C/7YGa42X5R+0kKDxbdVt4QKwwYVVt4WHu8bUXuJd5eZgq/G6+jLpwVzjrObsm1hvcMuay6Tf1XBiLHHNbeGLrcZR/fdwZrrBDepvC/tby8P7usFtYWCp8c//9iIhhBBCCCGEEEIIIYQQQgghhBBCCCF88gsqhc+pfeKwDwAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Вьетнам' AND delete_ts IS NULL;
    -- Германия (DE)
    UPDATE hunttech_country SET
        country_eng_name = 'Germany',
        country_short_name = 'DE',
        alpha3_code = 'DEU',
        numeric_code = '276',
        currency_code = 'EUR',
        capital = 'Берлин',
        phone_code = 49,
        flag_url = 'https://flagcdn.com/w320/de.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADAAgMAAAAIOVJVAAAACVBMVEUAAADdAAD/zgDGIigcAAAASUlEQVRo3u3MMREAAAgEIEta0pQmcHP6gwBUAQAAAAAc+plQKBQKhUKhUCgUCoVCoVAozAnnmVAoFAqFQqFQKBQKhUKhUCiMCRdwnu0eqEc+qQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Германия' AND delete_ts IS NULL;
    -- Голандия (NL)
    UPDATE hunttech_country SET
        country_eng_name = 'Netherlands',
        country_short_name = 'NL',
        alpha3_code = 'NLD',
        numeric_code = '528',
        currency_code = 'EUR',
        capital = 'Амстердам',
        phone_code = 31,
        flag_url = 'https://flagcdn.com/w320/nl.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAgMAAABbIsF9AAAACVBMVEUhRouuHCj///+QNKGcAAAAS0lEQVR42u3MMQEAAAgDoJW0pCktsU8IQKYsQqFQKBQKhUKhUCgUCoVCoVAo/BxumVAoFAqFQqFQKBQKhUKhUCgUCl+HAAAAAADQc7YcGxuYEkJ4AAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Голандия' AND delete_ts IS NULL;
    -- Греция (GR)
    UPDATE hunttech_country SET
        country_eng_name = 'Greece',
        country_short_name = 'GR',
        alpha3_code = 'GRC',
        numeric_code = '300',
        currency_code = 'EUR',
        capital = 'Афины',
        phone_code = 30,
        flag_url = 'https://flagcdn.com/w320/gr.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAElBMVEX///8NXq9Kh8PD1+uSt9socLgEkufSAAAA20lEQVR42u3asQ2DMBBA0ZMiBkDKBLAAEguQjJD9h0kZClNZPmzl/fqK197ZMZd7xq9pvrEABAQEBAQEBAQEBAQE/FPgZ0moBnhERoCAgICAgICAgICAgIAjA/dy22nkcTHzTtmLK3qlXBYAAQEBAQEBAQEBAQEBRwa6LAACAgICAgICAgICAgL2CVz3hPxEBwQEBAQEBAQEBAQEBGwJrPimnFJIkiRJkpT/XtxNgICAgICAgICAgICAgEMDl85zGJAkSZIktaz7vdjpAxAQEBAQEBAQEBAQELBdX3btcIP63HAbAAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Греция' AND delete_ts IS NULL;
    -- Грузия (GE)
    UPDATE hunttech_country SET
        country_eng_name = 'Georgia',
        country_short_name = 'GE',
        alpha3_code = 'GEO',
        numeric_code = '268',
        currency_code = 'GEL',
        capital = 'Тбилиси',
        phone_code = 995,
        flag_url = 'https://flagcdn.com/w320/ge.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAKlBMVEX/AAD/////Pz//qqr/4eH/0ND/vb3/8fH/cnL/jo7/nJz/GRn/Kyv/VVXjXqOqAAADp0lEQVR42u3dwUtUQRwH8K+ala6Bo6vrrgrqKTBBDwl2cg976qK0q9BppTS8uWSerS65ELQIYZ2SKMzTKlGHPGjXENRzf0yH2ViSl/v2/X6jI36/58fsh33zZubN4/0ejEZGEZAmlaZBIIEEEkgggQQSSCCBBF5hYCJbMMaYzLQ+sCttjDGJTEEGBGI/fwGD+sAO7A3dKQMyoCnbH3uuD+yxR8WEfXDANlPSBybtUY1C4KptZk4f2G2PuikEvrHNpPWBcXtUixBY+WkXw4w96roQOAkAaHMB3AcAXBMC2wEAt1wAjwAADUJgX6ieHAlor78DIbA/1G9FAn4AABwLgXY4bXYBnAozBdQEJkP15EjAyTBTQE1gCgBwwwVwCQCwKQR2hrrUBMCxiMDk4cvZU8D44ouvGsD7t9+mqyPYnDHGPNv6XaoTmAKwW5zPrgAAvi/nFk/K/z/VdQHbARyujWd7AQBPMjNbw2f8kzjz1J5OgwawN7DpuTqB3YGt7GgA+wKbTtcJ7Aps5UAD2BHYdKFOYDywlUF/gInzBhp/gP0qwLjvQO8vEofDjEtggz/joPczSQpAbGRtewIA8O3eu9f7UJyLYyOv1vMAgPXtjZNyhLk4ubt2t/DvamZl60RnNTNSfFxdzYwZYxIPNoY3RQtWh+tB6YI16W5FrbPkd3hPMqUC7Am1PxEJmFe5q7Ojaqu7++JBIbDX950F7/dmpnzf3XK+P9gkBHq/w+r9HnVll39TH5jS2eX3/TnJ33unY31gP0Jdf7WA78ezheWZCRen+PO0SWQefSnITnG48HEsgQQSSCCBBBJIIIEEEhgROKSR/SBgm0rTYBiGYRiGYRiGYRiGYRiGYRiGYTyN90+a+DCRQAIJJJBAAgkkkEACCfQX6P87TZUfO9YH6rwV5v69ujYhcMA24++biau2Ge/rDxb0gZWXHluFwDyA2i9gXnj9wUYXQNu9r0j9wRYXQNu9Pa4/OBpmCrjI+oP5MFPAxRdILAmBLJAYtd7MudUfHDNGUH9wIffQnuLcjHb9wY8Ltg9+yi2w/iDrDwaF9QdPx/gDZP1BP/og6w9Kx8FLUX9wtziffWpXMy5qAdvFQtRawMnD4mx1uXUOqxlZ/UGPC2Z7v6L2/p6E9Qe9rz+4IwQu+b4347z+YLMQyPqD0vqDR872qDt19qgru/wlfaDSV4YuQf1Bd1+6+rEn/9KV998KCxk+jiWQQAIJJJBAAgkkkEACo+UPMFt0EQovzWsAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Грузия' AND delete_ts IS NULL;
    -- Израиль (IL)
    UPDATE hunttech_country SET
        country_eng_name = 'Israel',
        country_short_name = 'IL',
        alpha3_code = 'ISR',
        numeric_code = '376',
        currency_code = 'ILS',
        capital = 'Иерусалим',
        phone_code = 972,
        flag_url = 'https://flagcdn.com/w320/il.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADpBAMAAACn2vMLAAAAIVBMVEX///8AOLi/ze0/acnr7/lmh9QQRbwkVMKou+eJo9/U3vNC96qEAAADF0lEQVR42u2dTW7aUBSFLWIKYfY1/CUjYqnzIKS2QzpJp6ErqNUsAKsbCOoGwg7KTjt4Nj8GC2bvUJ0ztCefLPu8c+97F5LEsizLsizLsizLalYmrgRxGdCABjSgAQ1oQAMa0IAGNKABDWhAAxrQgAY0oAH/X0D5JrplWdZ1qTURB+wsxQHX99p8aT6cSwO24UUacAMP0oBTuFPm6wK8KZsMgLLRrAGEjSbNAYSNph3KCF2j2QRAXaOZBkBZo+lWpaKq0XyoAH8Km8zdVNdoUoCHDYCm0bQBXtq6RvMIDOdpDnyUBCzC27cGRrImsywDg6LRPANMkqQF8KpqMtV6Img0KUA/SZLkG8BfAaQ/s319Afg1m81mnwF+H9z8GndlO69lvM/2MkX6qKeX8sXKX5tLAWMl2PalgLGW5lAjnVe8Kmp9GWA81+4A/MiyLFsDo93uSw7cZ1mWfYpbyXe3+b4AxrsbqyrRfI+cHKpl9xbgaXf9BuB9f4GOaTTzMsnsLb+9MtGkscvkKt8vgMH+jfJC9Pxf5vveUQQsH+lj9FZNyPfbV26r8qUsokfDkO9Xx2VIDoy78duFId/XTGZrNFUVELtfNKqZzNZoCoFO0jbR1DJ+L3aSqSeaQf3GAo1uZpVoXk9WoQr94FUAea9fvw3Xx9Fruw6c7nUUaOxJtOB0tyg82kl0wPA1PB1fvzn57cQymknDo1Vo+E/Fn6D8Oyj/Fav7oPxKIr8Wn00zfQGTacyDefw8KJ+oG2uSQqQmUa/q5Oti+c5CU2+m2m3qxU40Td2tjkh363x/8Dluf3DXYV0Aw8MO60Cgw6reo5bv8svvk8jvNKnv1cnvdsrvF8vvuJ/I+P39BVpNi/1THwM9Pv1zM/Inj+TPbumffpM/P9hTP4Epf4a1amhpmsxBengTBZQ/ia5/ll9+GkJ+nkR+IieEbOWZJvmpMPm5Ov3JRPnZTvnpWPn5Yv0J7e4ksSzLui75jwP8u1sGNKABDWhAAxrQgAY0oAENaEADGtCABjSgAQ1owAZA/3GAZVmWZVmWZVnWFesft5AYZyN8mN8AAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Израиль' AND delete_ts IS NULL;
    -- Индия (IN)
    UPDATE hunttech_country SET
        country_eng_name = 'India',
        country_short_name = 'IN',
        alpha3_code = 'IND',
        numeric_code = '356',
        currency_code = 'INR',
        capital = 'Нью-Дели',
        phone_code = 91,
        flag_url = 'https://flagcdn.com/w320/in.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAIVBMVEX///8Eajj/aCAJBY41MqLY1+29vOFdWrWZl9CAfsXs7PcP8P97AAAEQElEQVR42u3az07bWBQG8E9K7MTezSvME0T6nDjpypIJlFlZSoCBlSUDZbqKZGgIq0hh+LeKFChoVpZSCvOWs3DTkhnabU4139kg4s1P9r33nHvuxa/GAwIKKKCAAgoooIACCiiggAIKKKCAAgoooIACCiiggAIKKKCAAgoooIACCiiggAIKKKCAAgoo4M8GVBiLxz3Lut1hTAbDO6O855xfIryx6Nvni1i35+tzKbrWfDskGcw6e4+zTkySJ7Z8dZLcTN2//OLvxLklybEln5+TZPoIOAmQuCQZWhuAJ0dwATcFUlzs2BqGLsmTAvBK4BTwd0imZoA5GSAFvATuGM4YcBCTHUsvMMAz4E5RH8NLgTFiQ6+wR3bfo54AEbwpqoAzxuc+2TQyhWMGBeAPgC68KfpADYBT/mwgqmRzC8ADMEJtgHvgCsBWjxyYAE7IqZsC8wTnqA3wFk4EuKlHtk18YZIDXABehCypDZwWqmPgCDWSFr6xR44KeGP4bVSm1chrYALUp/DvyakB4JwEgBEwS6pRNapGThsYAgD5xsQq3S4AfDrDPKo1K43eoNrAUwTAn1hIyA4ZPSeAv1Z47Xqn0piNJ2N/rQCcmyqZWBiCU/yRAPOuz+2g0ogPiP4bwPlYPjOwCkbwhwkcprOIWYuf2i4TOMOifGYgz40BN0yQtebtPGuFk0bWhJOnQN1CtpuVhqcwcXkdZOdvW/EVUyc/A4DMQEUTf/mKWZhMWvn+/Wg9zNpO3gIAVBgYyCNnB2XVH+6yPxrdX3Z5l4cFAGw/rT6XuGTq3p8mgMt21m1u3vb6rQlTwL0cpe7qa8I6mQK78cZF8cSTjXNy/XiHZ/7RcXAHuKvf3HlkA4Bzy+B0N3hHkh/iu8uYmwmAyuoXwtqi6Ps9Znh4TJIbhzmDPxdPVw9czFP/Mg5mJNmJg9Pi6xxfdc1aLesBf+vw6uHgMSfJcG/7+upwqygriVWnkgo7n6+HcdkxCso/i/+GD+9nKweaf4PLYzC3NwaXZ3FOkmtLs3jVQPPr4ItM8u5bJvlgJ5OYz8U+eff9ambHwM74lXpwYqkeXKqoH8qK+tpURd0jp4s9SaWdZ+vhpJG1yj2JZ2FPUiUbi13d5Df2mi92dRULu7oX+2KY3Bf/qLNQsdBZQM5OAaD6Sm9mZuKwZF6uda90t3wb3S2PPH29P3hpoz/4nQ7rlZkOq/ke9Y+6/JEJoPlzEvTIZvLvkyYnM3PSZP+sDjPjp52ok1wvls6L921dCsheO3Fv2fHBj/97Z8HKFC7jiSQ7N+7YL+rJ8y1p5aTza5T3juJw7zEPbN492l++eWTwbpT1u1vAc7zgBSZvvwH+ZUyS33pbFo3bB4Z1/9P4xXgIKKCAAgoooIACCiiggAIKKKCAAgoooIACCiiggAIKKKCAAgoooIACCiiggAIKKKCAAgoooIA/G/AfMxFU3rC1xRIAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Индия' AND delete_ts IS NULL;
    -- Индонезия (ID)
    UPDATE hunttech_country SET
        country_eng_name = 'Indonesia',
        country_short_name = 'ID',
        alpha3_code = 'IDN',
        numeric_code = '360',
        currency_code = 'IDR',
        capital = 'Джакарта',
        phone_code = 62,
        flag_url = 'https://flagcdn.com/w320/id.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAgMAAABbIsF9AAAACVBMVEX/AAD/////f3/sDLJDAAAARElEQVR42u3MMQEAAAgDoJW0pCktsU8IQAIAAAAAAAAAQMmWZcqEQqFQKBQKhUKhUCgUCoVCoVAoFAqFQqFQKBQK34YHXHs1Zm0258oAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Индонезия' AND delete_ts IS NULL;
    -- Ирландия (IE)
    UPDATE hunttech_country SET
        country_eng_name = 'Ireland',
        country_short_name = 'IE',
        alpha3_code = 'IRL',
        numeric_code = '372',
        currency_code = 'EUR',
        capital = 'Дублин',
        phone_code = 353,
        flag_url = 'https://flagcdn.com/w320/ie.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAD1BMVEUWm2L/iD7///9jvJb/r33nPMM8AAAAh0lEQVR42u3OQREAMAgDsFqYBQ4F8y8OD30nCpI0/hT2NSIoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKNgGD0G5ujtNVTi+AAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Ирландия' AND delete_ts IS NULL;
    -- Испания (ES)
    UPDATE hunttech_country SET
        country_eng_name = 'Spain',
        country_short_name = 'ES',
        alpha3_code = 'ESP',
        numeric_code = '724',
        currency_code = 'EUR',
        capital = 'Мадрид',
        phone_code = 34,
        flag_url = 'https://flagcdn.com/w320/es.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Испания' AND delete_ts IS NULL;
    -- Италия (IT)
    UPDATE hunttech_country SET
        country_eng_name = 'Italy',
        country_short_name = 'IT',
        alpha3_code = 'ITA',
        numeric_code = '380',
        currency_code = 'EUR',
        capital = 'Рим',
        phone_code = 39,
        flag_url = 'https://flagcdn.com/w320/it.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAD1BMVEUAkkbOKzf////ecHhVtoPuyEaFAAAAqUlEQVR42u3OQREAIAwDsFqYBXCCf1F46O2ZKEjSeKdwpxFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQcHF4Af+oAFC7MPHDQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Италия' AND delete_ts IS NULL;
    -- Казахстан (KZ)
    UPDATE hunttech_country SET
        country_eng_name = 'Kazakhstan',
        country_short_name = 'KZ',
        alpha3_code = 'KAZ',
        numeric_code = '398',
        currency_code = 'KZT',
        capital = 'Астана',
        phone_code = 7,
        flag_url = 'https://flagcdn.com/w320/kz.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAIVBMVEUAq8L/7C3u6De52lWa0mgUsLZEvZp8y3lgxIortqjW4kQpsv1HAAALKklEQVR42u1cy1cTyRu9k6RfWc2tNEnoFR1AHqs0hMe4SqOCsGrAB7rqKCPKKhkERlZBRUdWHWSEcZU4Ko5/5W+RoKiZBV095/Q5P76FcuKxuKmq737vAi7kQi7kQi4E8Dp/K3EFeBAYY3Ofq0oprgCTg2mLhd5Ub1wBatlEz2oym2jGFaCaXe5FumepGFeAOo9NNIpObAFqolqA+8iO7REnxVZtz1pp9cQVYIJj7olp2fm4AmwMPuwXAxOHZlwBVq5NDbN0spWJLcC5wgvOv5uOLcClwXqKI8ZqbO+gLkqtE3G1FV+itvne2ycn4gowTU7j0GY2rgAVe5SzHGU9tpbEDN6KvHJvJLZE/aH/DScWtuNL1NNihHMivjxYGXyS5dXZ1dju4E+ZZyUxPFyLLVEnmamc5GYYW3crRd+tF9CKrcOqi5sF1Orxdfn1/FEeS3075agzAdFFdUt9q6l8OpodDIBXgBFp0NRT2xzds5J9kaxWBKaAo0hjkr3Ciii6ry5Fsn0nQD9QinITH2mmKoqN5or8UhPAKAwBwwQOI3Spe1YKW8koeLoEsK4xSGagfIoOYG2WgvNWBOfr1hU2U6w3Mkhmoztl+3iDJ1VH1h98DtR6FWbTLLsZ1Hpg/BMRD5qA7aMia0kWi6hYHnNpVp2Myp/xNiLblOgFbB9pWZc/aXkNVt0RddzbzSdZVO1qNAB3J+qw68ZDqUuoABqLCVYND4DhJVk+oBeNVWmtTcCua81WIHNPfoHCjDr1BfCVm3YGWA/k8RnWOoe4wKGalDEe24RLDwCUvzwA0JnFYS4SHVk3W+TOUEPKGFeEv2wCOLpsU7x/BShOWeNgJAD18i4zalEOYIo5eDBm2JYhDwqcSEJZ1YLvWuKOJ3HEOqDY9KC4PJUhQGcGUHx5JZlc4ywnVxl+KfV9HbsWlN/4VYZhcAS3L8vr8QFp4ikpc6F3xRN1EymeFR9rwWoUt1BZeB9A2flb5iw00ofifAMwB2gUEfDMMjmMt6SUP+iyiSS/lSoWI8lIOZkFvrL7ZxhIbWEV7ncA80gKeRWBwcBzMuJPtEJr8dUXAY6hf4ePIlD2cOvhtDQPGuVdZtRypRieAzkNLH4PkE0oOxR1aYDLwy3y2SUJor5H+j+cMJlHipR2Co3Mb8zwMjO74YnacOgbP+CjQIoRWOPW0UfY9cOmI6Ek+sAPOkySVWUsAprZvVaHXTceSQYljS4Aowm1Ez2A7SMZmrLmroyPB6h1AdgLfXx86ndZLckBto9G6AjikGQTrS4Ac1gkxaasqSvcus9PvhueDm7bbCpd8NHCIi15l6u2RcEJmSuojftqN4CENh6BLVnqWytcT0lmFvSuAIMolCTQ8rooLvZJ5AGOt6D9C8CXe/Ib+Kuli2JtbyrsArdmyCDVFaCvkSeyh5wadF9Z1UIqLGkpCyTrya4A6ymSU7I0k11sbmi5RHhTvDZmed2P2Dfs96+ko6b8YRaJwXW5FLD23ymJzjsZ7FbDZPlPc57KS6+7Fnvq1qk/EZ4ONeG7/rubYeokjn/qzBSNbvgEEhxoh3Wp8M5/klONafN+mMaethNuuP9m6kw0yCEPABLh/a6EtXHFyg85Ib6i01R9YJ8U1S7+KplFkhSTgOY1whuCxuCjSyzNh0n0uD1fm/oqXQB+OZRKtRb+iCuT/b183H89RDm2lv+pjUHd6hKSkEW8bOtxreyE75+sfBx9wfmxuXMD3EbF/CkPAIrLQOtuSIbbd6FsN5UnYe9g3l/miLF+7kvCYkIkzE72pA77B3wZpMgiAIWzLC+FrZirLNnTLNnnjpmcXIrrAoBu0/Sw28WhNhxaAaDyMX0nrJqkyAGsh4gPK3zAq/SBjesPPHSJmqqAcmt7FkjyqaWxGZoHOeDthwCY4oeWyfbGq6O+0vrhhLXRNksv0s03Qqf3DLtkn7B0/qKBYZu7okMl99jEwXcAP2CRH9pKTHuwFbppI50NkmJEeXteotKDXe530ooqWf7eqRYBErQ8AIpNPuagHoSlGb7gfOHcNJPOJ2mSwgNwwKG2Mp+RCUBx2Wx7OsKl71RDE/UIr/59/byNPQY3W6eqUJjG8jC+uYUmMFNVNkwAS6RgLhk2LGv0PsqzNP/63Heklj/o2DOtDrj0oH9FmPGhMw8c+2jH9BNO2OawhJi5YuUz53cWUvyDpeu22WFTAWDllK0tHzDaxw/DJmm9Da3F4d0tN1drovOL7/IfbRJQF0iSJwGw4d9rc1CSJIfDt0GHd1iTfJeDYQ8CQG0INXoAjh+Pz/0KQGWv4mbbJNO/IRyGrn3qvGOFdPkdCuAgA8AY8GALPPryTy9VmjAuA1A5DE2myqHnDrNIDO6HyF+myAAqq4DuQ2U29aX2lWLdFYDuAwcZD2lKVMX07GJzVcuFaOy577uzHtDxRVPCT7B8/xr+xOuJNIta+3IqrTJQ46XD0NmzVJ+7N7pXuHH+wD2ZeZgFkGwjSYygwcDOp1h3TJ19aBf/7pqAcf3davg7ePfPgi6Ktc0Q5QKXwgfQugQA6wF2c2C+3VTR6oHeB0Bxim1jF75/MtDyhigvhUkeaTYnANylD2ATqBUVDiZZrWRw0As8AJAUHoC7pEQDw1LfyrutcOm3G6MBAKXV+c9jHlhO0U9noLY/UpwsAGWMH2UTmNfCWcrbQx6At539+QjQU+npJjCHdjBQBIDXpkRVVincWeNkuBSw+mDZBKDUMh4A1IEBKBYwAOhtjh1oK7hMHliTSKJrZAYAjJ3hzicfgPfAy853d0wPgNEytyUApmXKECtXOlMU67+0P6gDW53tA56aQccYD0kAlCvk3HKrnc269oUWTn9Y79y8fTErk35rHU0G9t5auFKYMm4P+B3j9j2K+5n2immxJZMFbhcTndDFxJVW9ZS1P51VVWPmVDMaFNdk8pfm8rBDcT9sOdY5xZEireunEI3n9mmPwaG1JdX5IVvQNjYKnbN9SlKUfn+A48eX21FdZwNLUgAN0WkJcMIdMW47nWEj5fn4+GUyo9okRamTPD8afyHZOuMMbfCVPbQTNuOt7Jy9YaqTvUeWvp7q4i+QlGXyE17LtKXsz59FOGkPnVXaGmX7HJWFv/32H6FX2D/rqh1849y/luLojj2Xb406Kn35ekbB/yYoMKWbt+yT55zlyTOJ5jJA3ZjqXOF7Z+fzjgrT0vhU4QWuJW56bhkRyI2zB3HnQQQ9rLqpRtHg2JHD/kiqX98CXB+wyYWhSgQA1609P2qAirXOE161czX5tudl6/Fn/6vyFT6Pz72QR+g8jKBNueO//OEI7wtjd/oqpGW36MOuG7fkl9I5yrzhnSZuSL5jBEMWUbXKA4mWVdre/v3lto9OUvDa4yAKLYlm2ABQ35z+9ODhDMnPEU3cRjSucUo0p1lWQf9lNEvW5m3BJ1Y0i90gBQVtwZHxqIgm0bNSmE9GNNrp5txLfGY1RvYjY+xHWk6PaOgKUMaDmTfDqX/0CPk6vRnZ2BoAwFMiHk2MdvDvPxC9PTqZiO3wqZprD5/GdzpW3BxFre7G98WeuA9Ax36EfNFcLYnhj7/Fdgi/0nyS5dXZtXg/BLEd44cgGh8G3nBiJ75PaaQ7j5HE9jkXwx7lE3uU1bgCTFJM4rDFS3EFqLb43ntGbsYVYOwfZVoa9DU2jcP4EnXnYbD4EvXE1CWWpuL7tFqj99gUAxN34/tiD1szAzkR3+f9kmJrdy+z0orvK6Ki+g7uX634Bk08zqFRju+LPbF/5lTLJn6O9UOxyb5kvJ/aXQ+UsanPfnwfK479c88XciEXciH/v/I/GpF/uwyoK3oAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Казахстан' AND delete_ts IS NULL;
    -- Камбоджи (KH)
    UPDATE hunttech_country SET
        country_eng_name = 'Cambodia',
        country_short_name = 'KH',
        alpha3_code = 'KHM',
        numeric_code = '116',
        currency_code = 'KHR',
        capital = 'Пномпень',
        phone_code = 855,
        flag_url = 'https://flagcdn.com/w320/kh.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Камбоджи' AND delete_ts IS NULL;
    -- Канада (CA)
    UPDATE hunttech_country SET
        country_eng_name = 'Canada',
        country_short_name = 'CA',
        alpha3_code = 'CAN',
        numeric_code = '124',
        currency_code = 'CAD',
        capital = 'Оттава',
        phone_code = 1,
        flag_url = 'https://flagcdn.com/w320/ca.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAKlBMVEX/AAD/////8vL/MTH/RET/cHD/ycn/DQ3/WVn/3d3/srL/Hh7/nZ3/hoZ5FJ4sAAAEtklEQVR42u2cz2sTQRTHv7ppN23aw6ZpzKYJdLHWQxG2B8VjKooHL/HHsUKjB0E8RKkHPTV4FKF7EvSSXBQPQioiCh7aQ714aQVBBP8XD5ukm+xks5m6b54we0m7zPflw2bmvTdvZgeIeVn/+MK/vjSgBtSAGlADakAN+F8A2h5zwEKDOeD2FHPArTRvQBvwWAMWgAZrwA1gijXgFpDmDJgHgBpjwBwArDEG3ACAScaAFQBI8QXM+01rbAFzftM1toBNv+kkW0DXb2pwA8x2Povdtq3ODYcJ4Nxe57PbdtX/v7TKBLBg1IJdEJj2x7TbYAJYRtoLdMFOJ7S3cMgEsAg8dgJdEGhZVrZ61BeVDxIAXy1r4ajxqmV9B8BlkFhtABesg6PGs9ZdABk2frACABeDrZ8BIzJDUsADsWSWDWBTLJlmA7gtlkyxATwlluywAcyJJWtsAOfFkutsAItiSYsNoC2WeHwS1rpIYTLKqCsiRYoRYFWkmGAEuCtSzDAC3B47kBADCkPJQwaAWW9guiQIJLajELD0ISKUdALJyp5CwAXDfzwlkcIHy7o7CgGbnd8xL1LUOr/+tELAStcbD1dUsKkO0Ea3qO+GBUa37C+OyQkC3r4TTLMmusVzcSCpBtOu5Z8kgFVc/Ob/dRaAXz7YHRJIygDwyG/94Fkw+iUI2ARgrHtdrJlecbr/mrSCLex9t38alSCgHzbMK+/y9V5euhgWnOhlsmbt1a36YHBJELAgeFSCULIafrBmgwSwKHB4hbCgYVn5+vBJQJJuZvBrn3QGQ//TOrSslxE5dpKAgz4l44lCSc2y2wO30kSAoUrHBzHgSlQtJEnAs6Gg4SyFBZeyblSGmCRgeER8bIcFmWuicUMCWJS10SICzEqaMB2qbGZLzkSaLN3alTMxQwa4KGfiJBlgTq4LrpEBluRM7JEBSg5jh25OUpGxkCKcNLkyFgw6wDNyJm5QAZ6rS0aSHzSAsnz9hMkByvP1ESYHWD2OlQkCwPJxrBxS9MG38kaekgwS25W1YXg0bmZJ1sYlKkd9IGdiliySFKU8jdmii8WvZSx8IUwWshLpTMqhLAFLOMND2hr12M7wKXER3W6PJ8941FX+JXkXSLQMMVbSMKFgnWQcZ2i2VCzkvJZ1gVSA8Z1hylGzFDYfV3td1VpdM550WtliYj6WM8zU1C3HnpZygZSbKqoyLpASMEada0/ttpR7o3SfFO+bGTWDMhzVG3sK0bKG+p1HkSHZZLA1qh2/JqgE0I6WecoBy2PNRBQALkTLdpQDbkTLJpUDVqUCHSFgn5c57wLGzVh+hgowsHb8+bfnv6Ftv39+dLelGLAbSD6u+zu6XN/zLe9fGxFKqAAXAWBzvbffrNrrdMv7V4Hexi1lgLswXrwLjpngqHj1yx26WZkK8M+bgUE9MGzv3+b1Ej77gyD4A1ZSzAFdQwMeD7CdYQ5YN5kDjtFQCWA2+p1d9YDsD8RhD5iPe5qLBhyeYLdYA5aGlts0YLyvLSPy9Af1gPMj3irWgKMu9ieXsQfMxT12SxXgXO80HA0oB7gw4vgH5YCnRr1WrHyQXL7Me5DEvzSgBtSAGlADakANCAD4C/Si4oDBBIVjAAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Канада' AND delete_ts IS NULL;
    -- Кипр (CY)
    UPDATE hunttech_country SET
        country_eng_name = 'Cyprus',
        country_short_name = 'CY',
        alpha3_code = 'CYP',
        numeric_code = '196',
        currency_code = 'EUR',
        capital = 'Никосия',
        phone_code = 357,
        flag_url = 'https://flagcdn.com/w320/cy.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Кипр' AND delete_ts IS NULL;
    -- Китай (CN)
    UPDATE hunttech_country SET
        country_eng_name = 'China',
        country_short_name = 'CN',
        alpha3_code = 'CHN',
        numeric_code = '156',
        currency_code = 'CNY',
        capital = 'Пекин',
        phone_code = 86,
        flag_url = 'https://flagcdn.com/w320/cn.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAIVBMVEXuHCX//wD3jhLvLyH+7gL7wAnySxz1dhX5qQ392Ab0YhnZ2K8dAAADQElEQVQYGe3Bz2/bZBgH8G9jYic57ZvYy49TErYeODlCFfRWDxiFU0ylSdy8w0DdKYH+Ac4JsRMV07ajy8b/yetMLGlnO+Lyvo/Q8/lAKaWUUlViyOYtIdtpDrG+gJHmEKvzKEabU4jlJaNsxhXkWjMkC8jVpZFBLp/GEwi2ofEazvmo5l2z9CiGYyeo1ubW+QqOrWNU2tB48xbOpStUadGI4J7HJaqsx7894wjuBRyggvdZDI99uNfmEHVmQ7i3YIQ63QncuyFRp3cf7qVkgTpP4B7JHHU+h3M9kvcgWIfkBAcEcOeI5AgHtJZw5oZkiAN6EYyf4MKGRoFmPpfAs6/gQkIjRzOPEf7mFA70WDrDAQl/ZAgXOixNcEBKcggXPmFphANSkn/AhRuWQjS7olHAhQ23MjTwHrCUw5ZgvpNw64f5B8e4w3vArT5s8TZssMJdPz9MWJrCliBhrVeo4F3S6MOaU9YZoVKb0V+/cwVrZqwWFqjU4QDw38IaP2GlC1RrMYddJ6zSR41eGMOyNT82zlDD78M2/5ofyVHHO4N1Hd71PeoVsO8dbxvHkMVLecsU0rS47zHkueROFEOegDsDCNTlTh8CLbgzhkAz7okhT8o9K4jjcd8S4gTcN4A4be4bokkPDiy4dc6tCE26GexbsxTF77gVo8FiCvtSlqbwUpZWaLA+g3UeS68ABCwt0SC9D+sCGqMYxiWNARokI1jXJhmusLUhOUE9nyGsOyL5J97rJWSEGr/M5w/J715+/S2sWpN9/OuUJGqc8L0wg1UpwwwfzMgCNa64dQG7yAvs+Alz1PBSGn3Y1eMQ+77kPdQJSIYF7OqMM9xyNUGthGQGu45y3Oa9QR2fxhR2PcddLdRpkdc8g1xdPm5xArkW4xiXI8i1zgFsYoj1AoZfQKwY6v/KKyBbdwXZNgXEeg4gYAaxuucZnjKDWD7HxwkzyDWjEUOuLo0Ccvk0wk8h14al1zGE8hKWoimE6tIIX8SQakNynEGsgHzJMeR6+s2vPiPIdQxgFkG27giy+SMIN4dwPSillFJKKaWUUkoppZRSSimllFJKKaWUUkoppZRSSimllFJKKaWUUkoppf6TfwDKWIN5F5pz+AAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Китай' AND delete_ts IS NULL;
    -- Кувейт (KW)
    UPDATE hunttech_country SET
        country_eng_name = 'Kuwait',
        country_short_name = 'KW',
        alpha3_code = 'KWT',
        numeric_code = '414',
        currency_code = 'KWD',
        capital = 'Эль-Кувейт',
        phone_code = 965,
        flag_url = 'https://flagcdn.com/w320/kw.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAHlBMVEUAAADOESYAej3////vr7ar0787BAoAIxGBChcATCaPAmUPAAABrElEQVR42u3SSXFEMRAEUZ1a8rEomIIpmIIBNIVh0BSM2BFeZ/mLbsoIVyJ4h2yXZ3atvdOBnQ5sT3Rgu9CB6A0/gZ0OJG/4BQRv+A3kbvgD7HQgdsNfIHXDPyB0wytgpwOZG14DkRveAIkb3gI7HQjc8A7I2/AeiNvwAdjpQNqGj0DYhhtA1oZbwE4HojbcBJI23AaCNtwBdjqQs+EeELPhLpCy4T6w04GQDQ+AjA2PgIgND4GdDiRseAwEbHgCXL/hGbDTgcs3PAWu3vAcuHjDCWCnA9duOANcuuEUcOWGc8BOBy7ccBK4bsNZYHtb1DTwZVEGGmiggQYaaKCBBhpooIEGGmiggQYaaKCBBhpooIEGGmiggQYaaOD/Br4uataXWtSkb4gNDMGBBQem2MAhNjAEBxYcmGIDh9jAEBxYcGCKDRxiA0NwYMGBKTZwiA0MwYEFB6bYwCE2MAQHFhyYYgOH2MAQHFhwYIoNHGIDQ3BgwYEpNnCIDQzBgQUHptjAITYwBAcWHJhiA4fYwBAcWHBgig0cYgNDcGDBgSk2cIgNDMGBBQcm26cPQfMAvSNnm9oAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Кувейт' AND delete_ts IS NULL;
    -- Кыргыстан (KG)
    UPDATE hunttech_country SET
        country_eng_name = 'Kyrgyzstan',
        country_short_name = 'KG',
        alpha3_code = 'KGZ',
        numeric_code = '417',
        currency_code = 'KGS',
        capital = 'Бишкек',
        phone_code = 996,
        flag_url = 'https://flagcdn.com/w320/kg.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Кыргыстан' AND delete_ts IS NULL;
    -- Латвия (LV)
    UPDATE hunttech_country SET
        country_eng_name = 'Latvia',
        country_short_name = 'LV',
        alpha3_code = 'LVA',
        numeric_code = '428',
        currency_code = 'EUR',
        capital = 'Рига',
        phone_code = 371,
        flag_url = 'https://flagcdn.com/w320/lv.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgAQMAAABHbSffAAAABlBMVEWdIjX///941e63AAAAJUlEQVRYw+3KMQEAAAgDoPUvrRXcLdwkAABQmCNRFMVvEQAACgvG/fs9vLB63QAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Латвия' AND delete_ts IS NULL;
    -- Малазия (MY)
    UPDATE hunttech_country SET
        country_eng_name = 'Malaysia',
        country_short_name = 'MY',
        alpha3_code = 'MYS',
        numeric_code = '458',
        currency_code = 'MYR',
        capital = 'Куала-Лумпур',
        phone_code = 60,
        flag_url = 'https://flagcdn.com/w320/my.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAJFBMVEUAAGb////MAAD/zADYPz/lf3/yv7/VqhFmADOefiZrVTs8ME5KJ1laAAAECklEQVR42u2aP4rjShDGP3AgpI72CnsCwxcI2eleYa/wYoECsTMXeKFAwcOeRKDAjCcRODC2L/eClmyP3eXxspJaLFVgY+RAP6q6qusf0LN871mggAqogAqogAo4JKA5brcfze8A/uxZHr4s+o8kyV/V84DfepZH79rwLC8TBDQlryRppgYYZR3a+nTarrK4mRagafnij/bBcTUtwPress2UAPctn0u3zQQAo9a+LpZoCoCtgZ3hL6oeGnwUwFaBr7fPCwAI7bdXDZbCAXwDgCAFgIME+E/P4gwxkoE3ADBLAWDpM1nYkSQX93/MCgCzOYBw7hPQxuji/o9wCWC3BLArPAKGNsQ4nQdAvQDAyiOgjTGp6+0sLGBInwmrdZGbOGc1Vi6AMgFq6+GRF8DIGWOCyioXyGLAelCUegEMSJJ3Xpo3AGZMQSLgHIDJ/Zi4dgfBfQIg4AIkaqYAytQPoA0yjui9ACLSkA1ZATUbL4BGitI7vsCQL+SGBDZc+vHi0H0EAUO+ImNMZoyxp1uBw9/FgXSNYEcWbR62CCkocPhsZidmgoZkW+kllBQ4PGDt9pGrNJbSKR0HsJQu4nMe+yDbHgPQdY9Ea9ufqW8UaI7v1diA7ihzIJmvtte9kO0qJ/kGP4BLsc/wSeIKngDnUp3CL/s1QwNGEmBXy19k7cVJZECEn8wcF368OJTT6U8HUWx2DX3VPdDgdUNTbmkOnSzIgFH5hIsMDyh68eHei9+mBLhxxcEXH/mgM1DbfnWcX9jyTDDzOIA3V12Ur9bbU5crdn5+2r6v8mp0wEzqrJ7LFcr5zhiAD9Kt4Pr8pb4Ad66+wp0CZRUODjgTa5KATLqapBRVODhgIBqwZNJ0NYkppXM6OGDkTgiBkHGDc11sMhaeWsDSgKRkBUOeyBPZIBJUOHwTvXR7ScgCCJkYsint78IPoOAl9dttd2u/8AMYOsum6NXGoAZZDGPP6L7yMydxhrmjNX9y+QD48ANYipEQWWrhgtjnrC4QWxuGjW2iGzYeAQ2l2y5KujlJWfmc1YlziCAFsJufB3a+AMVJzqzpRmFm7nXamQnX3ftl2rn2utgTCp2XZ+bF46xGCQPj5gJo/AKGD4rzqJrCclktLAV8vfUxEmC31+PQoZnGel5rZP466+v9ucWe0fYHz+3A9QmAOeRPruiNt+C4+dxGKKa3gXn4ohvtf0X00rB8entw7B3W4ypjnK9Pf9MW8Jg7rAqogAqogAqogE8A/tuz9F52fp+4KKACKqACKuDUAX9MXHq/3HtPFhRQARVQARVQAf8McPJ3saZbCqiACqiACqhlp2YzCqiACqiACqhlp5adCqiACqiACjhZwJ8TF81mFFABFVABFfAvB/wf+ECtlRstLrAAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Малазия' AND delete_ts IS NULL;
    -- Мексика (MX)
    UPDATE hunttech_country SET
        country_eng_name = 'Mexico',
        country_short_name = 'MX',
        alpha3_code = 'MEX',
        numeric_code = '484',
        currency_code = 'MXN',
        capital = 'Мехико',
        phone_code = 52,
        flag_url = 'https://flagcdn.com/w320/mx.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Мексика' AND delete_ts IS NULL;
    -- Молдавия (MD)
    UPDATE hunttech_country SET
        country_eng_name = 'Moldova',
        country_short_name = 'MD',
        alpha3_code = 'MDA',
        numeric_code = '498',
        currency_code = 'MDL',
        capital = 'Кишинёв',
        phone_code = 373,
        flag_url = 'https://flagcdn.com/w320/md.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Молдавия' AND delete_ts IS NULL;
    -- Молдова (MD)
    UPDATE hunttech_country SET
        country_eng_name = 'Moldova',
        country_short_name = 'MD',
        alpha3_code = 'MDA',
        numeric_code = '498',
        currency_code = 'MDL',
        capital = 'Кишинёв',
        phone_code = 373,
        flag_url = 'https://flagcdn.com/w320/md.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Молдова' AND delete_ts IS NULL;
    -- Нидерланды (NL)
    UPDATE hunttech_country SET
        country_eng_name = 'Netherlands',
        country_short_name = 'NL',
        alpha3_code = 'NLD',
        numeric_code = '528',
        currency_code = 'EUR',
        capital = 'Амстердам',
        phone_code = 31,
        flag_url = 'https://flagcdn.com/w320/nl.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAgMAAABbIsF9AAAACVBMVEUhRouuHCj///+QNKGcAAAAS0lEQVR42u3MMQEAAAgDoJW0pCktsU8IQKYsQqFQKBQKhUKhUCgUCoVCoVAo/BxumVAoFAqFQqFQKBQKhUKhUCgUCl+HAAAAAADQc7YcGxuYEkJ4AAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Нидерланды' AND delete_ts IS NULL;
    -- Норвегия (NO)
    UPDATE hunttech_country SET
        country_eng_name = 'Norway',
        country_short_name = 'NO',
        alpha3_code = 'NOR',
        numeric_code = '578',
        currency_code = 'NOK',
        capital = 'Осло',
        phone_code = 47,
        flag_url = 'https://flagcdn.com/w320/no.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADpBAMAAACn2vMLAAAAGFBMVEX///8AIFu6DC/uwcrchpfS1uEWM2nZeo3jMjz7AAAA5klEQVR42u3asQmAMBRF0YCIdVbQBQRXiHu4/xSCVoKECAYVzy1/derHD315Q9hq46Flv3Z9nQIgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAg4JeAqbw5B2xSncL1zoEvChAQEBAQEPBXwPjyAAEBAQEBAQEBAQEBAQEBAQEfBJrfAAEBAQEBAb8NnMobc8B2qpPnMkBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEDAG1sBuuFYxrIko4sAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Норвегия' AND delete_ts IS NULL;
    -- ОАЭ (AE)
    UPDATE hunttech_country SET
        country_eng_name = 'United Arab Emirates',
        country_short_name = 'AE',
        alpha3_code = 'ARE',
        numeric_code = '784',
        currency_code = 'AED',
        capital = 'Абу-Даби',
        phone_code = 971,
        flag_url = 'https://flagcdn.com/w320/ae.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAElBMVEUAhD0AAADIEC7///+r1r+qqqoDfhjfAAAAkklEQVR42u3OQQ0AIAwEsFnAAhawgAX8W8HCfkdCq6A1mypFUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUPDl4A5pB1eIoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCg4N/BE9IOjhBBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBwYQLGzUbx1aa+HUAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'ОАЭ' AND delete_ts IS NULL;
    -- Польша (PL)
    UPDATE hunttech_country SET
        country_eng_name = 'Poland',
        country_short_name = 'PL',
        alpha3_code = 'POL',
        numeric_code = '616',
        currency_code = 'PLN',
        capital = 'Варшава',
        phone_code = 48,
        flag_url = 'https://flagcdn.com/w320/pl.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADIAQMAAACjyqroAAAABlBMVEXcFDz////vuDXSAAAALElEQVRo3u3KsQ0AAAgDoP7/tL7Q1QgzmVJEURRFURRFURRFUTwdAQAAgBcWbAyRQu0Nje0AAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Польша' AND delete_ts IS NULL;
    -- Португалия (PT)
    UPDATE hunttech_country SET
        country_eng_name = 'Portugal',
        country_short_name = 'PT',
        alpha3_code = 'PRT',
        numeric_code = '620',
        currency_code = 'EUR',
        capital = 'Лиссабон',
        phone_code = 351,
        flag_url = 'https://flagcdn.com/w320/pt.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Португалия' AND delete_ts IS NULL;
    -- Республика Корея (KR)
    UPDATE hunttech_country SET
        country_eng_name = 'South Korea',
        country_short_name = 'KR',
        alpha3_code = 'KOR',
        numeric_code = '410',
        currency_code = 'KRW',
        capital = 'Сеул',
        phone_code = 82,
        flag_url = 'https://flagcdn.com/w320/kr.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVCAMAAAARktncAAAAS1BMVEX///8AAAAAR6DNLjowMDD5+fna2dt9fX3Pzc+MjI0fHx/s6e29vsFoaGheQl82QIWeM1HqrLGUrMvhg4pfisLWXWcxaLDRQ07PNUFHQmOAAAAJCElEQVR42u2d65ajKhCFExVEMUbN9f2ftBNjbgYElEtJ1/5xzvTKrB6yrQ+KAmGzQaFQKBQKhUKhUCgUCoVCoVAoFAqFQhmqYNBbyArQrdtucwK5gSTfbgE/Y5Jtt9uMwm0g7RsI9xHfHu9dYCEpHu3LAQP8EMwg7MOvF1SIXw3cZti8+QCDfcTs3b58De3jkBrH4T9h8kFIP8xlGRgLedZDS8pRE0EDXMAZjl9NgQzxuG20/2MNIAh53TeFjp4yhQ7w8FMXumXd58j700zQAD9Uh21Y/WrIGGJQ6T4VAtxrF7ZluxG0MCGWArzdloF7QV5+p88EZDotB3hb9Z+HKNGQXQ9DtYUPsQrg28+l98yVlQOk8CFWAfz43G8QkvzVHPgQKwH2X6Jh2Qek0CHWAPj5MfEZfm9IYUOsB/DjZ18Gjua8RA5xvRqAvZZAxpBKId7xFQHsM6UeQyuGuKxgA0zCALwRQSuCeAdhNjzujyEArAMx3WZDe8KuExsAnId9smOIq/ffYwjwDIiHAMgCNO1DBVCAdSD+aG+OAM+BOPw68c+kEg7AWhAHnxOrAC4CryJOQJyPPs8RYAXEXDEnZgjwNMS1cI4cdImpkD9OGwAT2raHw+F00+1/bUtjg5j+pPb2AGaH0yUddL1ehz9dTuYuGkBMwQJsRgc/nG6upRJdT61Z/ST7fsxwIHYDMG+n3Bt0aok5xM/CC5NDXKweYHZKNXWghhC/Cy8wIB4DTCwA3Grb14chM2hqxiaq1UHSafsAm9nXW8h1IR6WYkguHHlpAIitA0yN7etB1gvuwej7Qh0TQRsAYgXAG1OAD+k8XVr9TicXLjGFgdgywPSSzpYux8/CCwiIqXydeg7A7TVdoKtWEObfTzQwxPV0Cm1WJSKndJmuGj2hElq/68S8tgcwv6SLpYExU8yBX9B42pJclZYAphb8u40l3IBhMcSF53VivrMyArNrakVXZc81zuyFEHvdpnALQmFN0gTgNrUl9VCihtj7DsZKkRHk/vxLrylbADF7fZ2QVWljgFlqU2qK5dl/wBceJgBWfSF6Te06yBdCHERVORdgfkktSzkWSyEuQ+7t2M0DmFj375YPEu3+5mtOHHiT1iMITQE+pA50mANx+D2C95zQFGDmwj91MiOAGMQewcp0pwS/pG4cVHWDPyMxgC2q0iKXb4D7blAfYnAnixgU1VjqTAYQQztYpNAGeHEFayqX0R+JwYXgKydUPdoudSjdkbisoPm3IUNOqHqy/OTSwFQvnd7BPP3p/oKksgpdOPVPGYJ3iEuwx4+RXAkwr90aqExlKOzz5ZQ1yWOThg1BWAcCmcdo4ti/9Eo2MavbuzYwbaM28Fw7N/AUs388cW9gSmMmOGncG3iImWAfBl5iJtiHgREz3PkxMN5x+OjFwOsp4i7QkoFN0/zHTvDWBS5OY5r9Phm039czSzJrVbXUwPpl3svEuvlHnWB3/8YL7EuE2je/my7jHUOS2cWEJpHqx8JYDTwnsxlu9smU6v8xHX6Ei+XwS0S/NU7/yOO72uv9vvSFcZw1QS7ETUP7REfx5zE0mReCev59/V4atYG1fX7H/WCcBrJkTgg2ibbeT4ZFaWAlGTKnlRioiXsqUsnSNhsd4HdsR46wQU3GzL/Xk4l7EDFwsE4M9S/SGP1usDH17xmCcRrIp2soFuLvFYKRFgQn5l4W+r+P33qNuRqTaI7FzSz/Hr801mrMUVkKXRh+T4YP/8RAuYV1MltNxAZ2ogns76JGvU+SZQbGuiZCJUWAjzhslrk3dIKxrsptpkopvZLlqmPeHHNM3Gsf8/aszo+B8e6NoX4MjLYL/Eml3RgY8R5f4oHhOupd5tyHgRET7IPhOupd+h7G4Sbu90SIcwMv696VoH5XzvUg3C5uYsj40nhb03UXqApAyG9rar0v7DgEldM4uO8LD2+s50FDcK+KLrhvrGufmeA0BFslwFDPTDA4tcNlAKr+bbindhgc/OQwF6y0AAZ5bozJyUXOpiNn1SgH8+SiKjM8/M7ZOMK1AR6OooXQE94H3x0MiJk+wHBOb5t3fqCTkbjTBxjM+YGzT7A8++8AIZ5g+T5D1RRi+93geY1nqC44R596HkBgnuK76Bxpu/4p07oS2jnS45PMja8i6HwOwOBOMhecpW98GUbn0T9gZ+lbus2h8sav1pUs/m5z6EpLN3oxP+MHsPtEbN5oQ33kL9ButLF6pxJfnFEfZ9ypVAa9U8nytXyd2/nbqFLUQ7GT323upchl+V65JUF41vzCX/fKVfII8FTkyvQh1spRO6fh96A41wHY1/Wk1u/WJLOqM0e9Hn98tyaEC3Lt3+7KjS08an7b8e2u4QF2AbGxhbr2Td4vHHBO7OSGa30Lj2u/4foX4uLbwJl3rJNOY0Q+dwbZ7s/Z+bX0ZlXPy0wyiCtx+Vc/DKc9PHd0ZjOf/nRAilpUOBt6T4Xognol7Y4CF8/nzrhg8gEoH01FgwIsg7gTfz6rUESr7njX+f6frpr1/SSAdgKA/VelfytCz/CrxhWjMtCyDZHdocRrCFXpMaSDfXPWid0v2dBhBH4Ox1xU5AoN8bNUPT+dtqsxwNWo5hd8WUlAwNc6cWCIf1Lm8Tqw/zmwCuJ+i+rSdNolwN/rwDQLCojOnDhkJyMEePQ0C0DrwuMdWqEhlgH8DWwfhAHvF5ak0xAglgI8/Fy9/17QLeeF/IrmoBBPADxUP57DMQu71RIoxCqA+2bB2KpvAPHOX6t2CoBzQFv1c4AQV0qAn88UwHtfACEerwNzeRW6XhXEvl5SI7kOwHBedtCEOPPYWJZNAAzuZQc9iP2+I/mxDjwBcLaBIRXELMQ+5Pu8fBUAa0EcYrZJdvkqAFZBHDhRWAHAOhCH0xoAnoY4cKZVrwDgSYi70C2Dsg5sUP4QrhMH7AVr+ABLIC6BUFKAB1gMcQbmhBaeZaI58gY2xKAOuAGxDmwKMfQnDE8wc9S1NO/zEWcg7/MAsA6sCXEBtX0FaICfI3EG+DqZ4OvAGhDnoM/vIzlggHtIwF9IxooNCoVCoVAoFAqFQqFQKBQKhUKhUCgUylB/avat7jo02dUAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Республика Корея' AND delete_ts IS NULL;
    -- Россия (RU)
    UPDATE hunttech_country SET
        country_eng_name = 'Russia',
        country_short_name = 'RU',
        alpha3_code = 'RUS',
        numeric_code = '643',
        currency_code = 'RUB',
        capital = 'Москва',
        phone_code = 7,
        flag_url = 'https://flagcdn.com/w320/ru.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAgMAAABbIsF9AAAACVBMVEUAOabVKx7///95ANL1AAAAS0lEQVR42u3MMQEAAAgDoJW0pCktsU8IQLYsQqFQKBQKhUKhUCgUCoVCoVAo/BwCAAAAAEDRlAmFQqFQKBQKhUKhUCgUCoVCofBzeLYcGxv1oYF2AAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Россия' AND delete_ts IS NULL;
    -- Саудовская Аравия (SA)
    UPDATE hunttech_country SET
        country_eng_name = 'Saudi Arabia',
        country_short_name = 'SA',
        alpha3_code = 'SAU',
        numeric_code = '682',
        currency_code = 'SAR',
        capital = 'Эр-Рияд',
        phone_code = 966,
        flag_url = 'https://flagcdn.com/w320/sa.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Саудовская Аравия' AND delete_ts IS NULL;
    -- Сербия (RS)
    UPDATE hunttech_country SET
        country_eng_name = 'Serbia',
        country_short_name = 'RS',
        alpha3_code = 'SRB',
        numeric_code = '688',
        currency_code = 'RSD',
        capital = 'Белград',
        phone_code = 381,
        flag_url = 'https://flagcdn.com/w320/rs.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Сербия' AND delete_ts IS NULL;
    -- Сингапур (SG)
    UPDATE hunttech_country SET
        country_eng_name = 'Singapore',
        country_short_name = 'SG',
        alpha3_code = 'SGP',
        numeric_code = '702',
        currency_code = 'SGD',
        capital = 'Сингапур',
        phone_code = 65,
        flag_url = 'https://flagcdn.com/w320/sg.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAGFBMVEXtKTn////2k5v81dnvP075tLryXGj0d4LTmi12AAADcUlEQVR42u2aMW/bMBCFz5JsraUZ26ultMhqmYa7ynCSrhGSIKvdoM0a10X79ztYVEiRngLkDsV7QwAJHr6Q9+6ORxFBEARBEAT9v8r+frmv9OPVH6F83yt10lMtEW903+JNfotcvrzFUzOZ25vb7R1L5xMZfjSyfBOXL5MDWNgA3LlvX8TwDS3f2nv9KCY/2w3WfgBWUgC3dgHv/MBUr0IcYvkm/vukB8y/gAv//UB9kgEYi8CsLMtCTcqy3Mmx8NR9e5BTWOI5sHW23smxyLhvkdDXLBqcsQjthXQ28R0molQpCTbOziRBok3fN5I8fFpaHVJ/vDbxMkdElb5uFD9gYwG71rXb+znlXWC+ykkyaVtPkjURbe26srkltYAdwWbeAtZERO1DPpUTgsUiZiU2t9iS+9b6VRfRbM7ukbrzxjj6f3DVZMun30qwjv4fdyJMfDSmUsoY4+IkxhiltDFLzjpy4Y0/xtFqPefMMguv+9/FTvVrEVmmUEqpVe9XN9Fm4kO07wOOol1+4eYhHkAbX3l0rQqmCIzk6dTtG7xktOBtp1+7oJxVwW6OlD4y9a5Bv3/QdThQSNRd1vCcP4NK18yJ9v3eajAlyrUIwGxNRPTU+9VzTUTbmhWw3eLM+fum2vkr5cwpRWE7KExBopam84dOAL5zciRFqd+wdrXNe+J0eBK/QUxcpoxz+8/MtgauqXPWGVzVP9Wdso/LlLJOMeOlpHB7qw3rDC6eqauxV21E2HjhN6heFHDWwWGQZ56NMUoZYx6I6MY+fGW3se4PG073Tg17MxG6JHWDsp01rEQVu8KdI2y578MiX6OMvIvFhrudDVN15j2xAx6CTJi4xzxib8bCRJOqSeXMQnTDfCMW3Gdv1G7YeWaoHkYV741YOIJbEe3teg5mRDe8t9rBXc6SiDJbOo41EV3KOnvu3Kb69FCTLJtIk/j5Qq5kfztI9Cz9eHzu6zI5OvN9niAdLeFnEL6bcC6V8NIaRewafrMnpKXUhE0/2nQzEUuY3b78uvp5TRAEQRAEQVJVClf3RaNUARCAAAQgAAEIQAACEIAABCAAAQhAAAIQgAAEIAABCEAAAhCAAAQgAAEIQAACEIAABCAAAQhAAAIQgAAEIAABCEAAAhCAAAQgAAEIQAACEIAABCAAAQhAAAJQiP4B7kf+z6uvGd8AAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Сингапур' AND delete_ts IS NULL;
    -- Словения (SI)
    UPDATE hunttech_country SET
        country_eng_name = 'Slovenia',
        country_short_name = 'SI',
        alpha3_code = 'SVN',
        numeric_code = '705',
        currency_code = 'EUR',
        capital = 'Любляна',
        phone_code = 386,
        flag_url = 'https://flagcdn.com/w320/si.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgCAMAAABKfUWuAAAAQlBMVEUAAP+Njf8ZBOa4uP9AQP8cHPlkZPf/////AABAAL/vARG9AEJqAJXj4PyTBW/19QzhDSnsM0f+X2Gqqlb4w8r/kJDP5OzMAAADG0lEQVR42u3a3XLbIBBA4QUBi0G/cfr+r9oLy7ITwJajTGeanHPRcdVOLr4CllYVpUMJBAACCCCABCCAAAJIAAIIIIAEIIAAAkgAAggggAQggAACSP8EcOg6Y4wxpusGAF+rM87Lh7wzHYA79cIF745wvRA6AJ/rXc3s2frio/x4w0OA7+G26vw0Tf720d7+ILwDWO9tuT/0NkB7ZykiIssbgNWW5cP3xrn6UUSWBcA64Cy7mgFsbOG9gGzhBmDaB5gAbADGEsu58loEsN6fEvA8TecS8A+ADUD72SpMU/h8zQK4G9CG8xnA/Q8i8VRsV1segaf4DuBuQAHw2wF7ABsNsS+0fA1wALBeBdCYCiDTmBbgWGipFpdGAFulAtCoFktwTADuBlQtl2CeAWw053IBlksQwGbLDTB01wWo6kWkuz2Q5AXAFuA2EPSqZl2A10/+V8xTjwFuA0E/qKrz1+veqerwK8aB3wR4eXe5vcFcf/sbxoEq7kDjOhAM9Z+9HoNxdD84OVJ/mWe51r+Ou0yzeqFHgL75qDt4AJ8Mqk7bAVit2/4S1bKx3+5d6pltn1P1heXYPgCvx+CYcGq/Ms+PF6CqkZxxajbG1i3MditjKzMvuv8WeThuHvgOeVx8cghyBD4pJ5EwqKoad/0vvV3w4oyq6hBEEjv48R7uRcQ5J+JC8OLXX9x6UXp28NMlGIwTZwZV1c75dRkOxoszQRLfwc+XYNfawh0LcM8StOJCZQuH4MRGFuDTx7mUxde3sJeceIzbMZLp6/MENvDeTVy/mx7kxAbefQyWM8HBW76B9x6Dc7Kfd7ERm2YOwP2Cp49ThSAn/F4RzLEXv022jJc+ZvxeaYzZroTGi80MsV5+JJnjKOKNEZExzty/fOGGMKXRitgxJd7Dfe0k7FPMOaae0+/rqzBnVh8RERERERERERERERERERHRd3WiQ0mkQwEIIIAAAkgAAggggAQggAACSAACCCCABCCAAAJIAAIIIIAEIIAAAkgAAggggAQggAACSAACCCCABCCAAAIIIAEIIIAAEoAA/o/9Bdpc8pjuwx68AAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Словения' AND delete_ts IS NULL;
    -- Соединенные Штаты Америки (US)
    UPDATE hunttech_country SET
        country_eng_name = 'United States',
        country_short_name = 'US',
        alpha3_code = 'USA',
        numeric_code = '840',
        currency_code = 'USD',
        capital = 'Вашингтон',
        phone_code = 1,
        flag_url = 'https://flagcdn.com/w320/us.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACoCAMAAACmLsfDAAAAP1BMVEUKMWGzGUL////sxtDZjKHGU3IiRnGFmLBIZYn9/P0RN2ZshKC4w9HN1d9xXYAxUnqaq75VcJHd4unq7vLHVHLv+YXxAAAEGElEQVR42u2dy3KjMBRElZtEbw2Y5P+/dRYzqWC4FjbCULT7LrKg0gtOSfKhQznGHDxvJx9DgARIgCAAL9r9dZ129XL3xcU8EMCi3atzGpVBo7IujwPQinavRWPlRGHV6fmykMcAGJNzWQbnnB1vNOeciHNuvDmtc26Q7FyKvxcb8iAr0BYREfm6PsdcEBGZrCz/JSIiZYyqmg/VPMwWdjMoxhgfRIKfXLRFRPrptuxX5rEATqlYEZmfd2W2Kqt5W83DACzDJc9YuZBScDMq+TKU+afNujwKQNtHY1Ka7svOmG66W1MyJvaTdbU6jwIwjn7+im68efHn6o8or80bOfcE9fFqS1H+nUF9KIEEqIqyXSfKC6KNB3BrUa7kQVfgxqJ8nfevsIV1UX6KaIOegZuKcjUPugI3FeVqHhPgtqJczWMBfIoo1/NYAI9ppHEAHtRIYwA8sJF+P/mwkWYjzUaajTQbaTbS522kIV8u2rORhgS4ZyMNB3DvRhpvBe7cSANu4X0bacQzcNdGGnEF7tpIf557vtlIs5E+YSNtS6NolxdqpHVRto2ibdEaae99L8V7PybTeX8RuXg/XlrWe1+k996PRXl9HmQF/rdfcXFuvxN9c6I49WV1HmUL26w9KLigKJ3/EpE8/ZS9mQ/1PMwZmEQk3yfKWURmQpLuF+2rPAzAHPoh2BmVnGesbBj6MGO9No8CMA6dif30XnMyJk1ZpT6abvpYG8tj+QFtC9tojDF2Irr25sVor0X5X757OI/5iq9tbKRviLZ9oXektxPlhTzKmwnPE+VKHrPOio2i/EAetQ/cVpSredBGelNRroo6aCN9U5Rla9HGBLiNKMd78lgAO010rdVEudNE+Y789FexAGZ74w8k802e1aNzVR4HoA3avWaNVVJFeV0eA2D03jtNlH0Iqii7uSivzIOswOMaaZgXLI9qpGHOQFV0YxAJDaJsl/MfJ59r0Y33iHJsbKQjZiNddm6k0b50QhfdVlFezoM20huK8kIeE+CWojxqbtJLAHxYlP1q0cYEuHcjDbiF922kEc/A+0W5VbQxAW4rylVRxwS4byMNBPAporzYSAMBPKaRhikT2EizkWYjzUaajfT+jTQMwKMaaRSAhzXSMGfglegaTZSN2ii35hFfb9t1CJAACZAAjwT45+A5fZkgnLY6iwwIkAAJkAA5BEiABEiAHAIkwNMB/OC0lQlvnLY6iwgIkAAJkAA5BEiABEiAHAIkwNMBfOe0fQEjGyn2gQRIgATIIUACJEAC5BAgARLgqwH85LT9a0gWUuwDCZAACZBDgARIgATIIUACJEC+mcDhmwnsAwmQADkESIAESIAcAiRAAiRATgUg+xTWWQRIgATIIUACJEAC5BAgARIgAXIemb/Mc2QBMzY8bQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Соединенные Штаты Америки' AND delete_ts IS NULL;
    -- США (US)
    UPDATE hunttech_country SET
        country_eng_name = 'United States',
        country_short_name = 'US',
        alpha3_code = 'USA',
        numeric_code = '840',
        currency_code = 'USD',
        capital = 'Вашингтон',
        phone_code = 1,
        flag_url = 'https://flagcdn.com/w320/us.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACoCAMAAACmLsfDAAAAP1BMVEUKMWGzGUL////sxtDZjKHGU3IiRnGFmLBIZYn9/P0RN2ZshKC4w9HN1d9xXYAxUnqaq75VcJHd4unq7vLHVHLv+YXxAAAEGElEQVR42u2dy3KjMBRElZtEbw2Y5P+/dRYzqWC4FjbCULT7LrKg0gtOSfKhQznGHDxvJx9DgARIgCAAL9r9dZ129XL3xcU8EMCi3atzGpVBo7IujwPQinavRWPlRGHV6fmykMcAGJNzWQbnnB1vNOeciHNuvDmtc26Q7FyKvxcb8iAr0BYREfm6PsdcEBGZrCz/JSIiZYyqmg/VPMwWdjMoxhgfRIKfXLRFRPrptuxX5rEATqlYEZmfd2W2Kqt5W83DACzDJc9YuZBScDMq+TKU+afNujwKQNtHY1Ka7svOmG66W1MyJvaTdbU6jwIwjn7+im68efHn6o8or80bOfcE9fFqS1H+nUF9KIEEqIqyXSfKC6KNB3BrUa7kQVfgxqJ8nfevsIV1UX6KaIOegZuKcjUPugI3FeVqHhPgtqJczWMBfIoo1/NYAI9ppHEAHtRIYwA8sJF+P/mwkWYjzUaajTQbaTbS522kIV8u2rORhgS4ZyMNB3DvRhpvBe7cSANu4X0bacQzcNdGGnEF7tpIf557vtlIs5E+YSNtS6NolxdqpHVRto2ibdEaae99L8V7PybTeX8RuXg/XlrWe1+k996PRXl9HmQF/rdfcXFuvxN9c6I49WV1HmUL26w9KLigKJ3/EpE8/ZS9mQ/1PMwZmEQk3yfKWURmQpLuF+2rPAzAHPoh2BmVnGesbBj6MGO9No8CMA6dif30XnMyJk1ZpT6abvpYG8tj+QFtC9tojDF2Irr25sVor0X5X757OI/5iq9tbKRviLZ9oXektxPlhTzKmwnPE+VKHrPOio2i/EAetQ/cVpSredBGelNRroo6aCN9U5Rla9HGBLiNKMd78lgAO010rdVEudNE+Y789FexAGZ74w8k802e1aNzVR4HoA3avWaNVVJFeV0eA2D03jtNlH0Iqii7uSivzIOswOMaaZgXLI9qpGHOQFV0YxAJDaJsl/MfJ59r0Y33iHJsbKQjZiNddm6k0b50QhfdVlFezoM20huK8kIeE+CWojxqbtJLAHxYlP1q0cYEuHcjDbiF922kEc/A+0W5VbQxAW4rylVRxwS4byMNBPAporzYSAMBPKaRhikT2EizkWYjzUaajfT+jTQMwKMaaRSAhzXSMGfglegaTZSN2ii35hFfb9t1CJAACZAAjwT45+A5fZkgnLY6iwwIkAAJkAA5BEiABEiAHAIkwNMB/OC0lQlvnLY6iwgIkAAJkAA5BEiABEiAHAIkwNMBfOe0fQEjGyn2gQRIgATIIUACJEAC5BAgARLgqwH85LT9a0gWUuwDCZAACZBDgARIgATIIUACJEC+mcDhmwnsAwmQADkESIAESIAcAiRAAiRATgUg+xTWWQRIgATIIUACJEAC5BAgARIgAXIemb/Mc2QBMzY8bQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'США' AND delete_ts IS NULL;
    -- Таджикистан (TJ)
    UPDATE hunttech_country SET
        country_eng_name = 'Tajikistan',
        country_short_name = 'TJ',
        alpha3_code = 'TJK',
        numeric_code = '762',
        currency_code = 'TJS',
        capital = 'Душанбе',
        phone_code = 992,
        flag_url = 'https://flagcdn.com/w320/tj.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAG1BMVEX///8AZgDMAAD5xQlAjUDZQED72Fn97Kz/99s4RWCiAAACk0lEQVR42u3ZQYqkQBAF0A8Fmtu5wpyg4UPqAeYKsxcCtJYJCVrLBMG69iystrRnuujVGMP8v7GrV49IDSNTfHceCCiggAIKKKCAAgoooIACCiiggAIKKKCAAgoooIACCiiggOcAfzoPFEVR/q3UyTmw6pwDL29eZQEA6t5inwCg+FtbAMBMtgAAfyud1wu5Xht3QEsAUJPrNTrjLSQjgKqdOwBG8uqsgGvpaiCthfRWwoqHuy7T21NyiYeSmXnrh7nk/c8meHuME8KuN4eCWnPJ11Nevv4c5NOOtzgZE/bPw/5ORHYxMkxjnG7PdvhsgPeJw+ShwfCJChwmlieWHppNsCfj0u1H1szoYo13r7Vc9rdkRR/d2vJWswgA3OrZ+JgYWgzvfzKm2jbgiNnJZmRryM0Ybci7Bu5ta5KQGzfvjz9nbrUvFvDvPxgvgB7G1uYVMDtog/EV0E5vhL2xv34GXHrG4WRgTfIwox6m190kduIsc1hUO6z4xcE8cwSGYz09AG3ZG7bzwYf+ev7A1WFcb8YOQGA0FgDV2h2Hx7mmiymBCTC+zYwFNf3NDGaMRqaKjMbo6fQtFAAzF66HmEaSV7YfN8lnbtwLRhJGsgulIhlBDgg+Nu4ASbKtg3HApcPIWJAf9XTyfJBMOQFAbgEgtDVJN09KIHmFvQFYyAHAJWL58A48N2NCbWyATDbATCbcB1/fFY3s3r+TVP4O+YFqNa2fY83dIT+AUMJ96vu+n24FN4fbpdG45fQx9ffyjfyQwdWpR3hn9X2/FdKVkCSH24MU7iNJOmsyxyUNo7NGM33pX4qi/Jf54Tz45jwCCiiggAIKKKCAAgoooIACCiiggAIKKKCAAgoooIACCijgOfkFJ0iFIeXFjOQAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Таджикистан' AND delete_ts IS NULL;
    -- Тайланд (TH)
    UPDATE hunttech_country SET
        country_eng_name = 'Thailand',
        country_short_name = 'TH',
        alpha3_code = 'THA',
        numeric_code = '764',
        currency_code = 'THB',
        capital = 'Бангкок',
        phone_code = 66,
        flag_url = 'https://flagcdn.com/w320/th.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAgMAAABbIsF9AAAADFBMVEWlGTEtKkr09fjMh5QqVVoUAAAAW0lEQVR42u3aQQ0AMAwDsZIcyZHsSOQzxQfAAKLMSJIkSd1tuLnhgEAgEAgEAoHAz8ATDggEAoFAIBAIBAKBQCAQCAQCgdWg6R4IBAKBQCAQWA7G386SJElSdw/f+IVbMggMPQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Тайланд' AND delete_ts IS NULL;
    -- Турция (TR)
    UPDATE hunttech_country SET
        country_eng_name = 'Turkey',
        country_short_name = 'TR',
        alpha3_code = 'TUR',
        numeric_code = '792',
        currency_code = 'TRY',
        capital = 'Анкара',
        phone_code = 90,
        flag_url = 'https://flagcdn.com/w320/tr.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAGFBMVEXjChf////nKjX4wsXsUFr84uTweYD0naKRXvWHAAADVklEQVR42u2cQXPaMBCFheLAtSoYrnZDOcclE66kNOUKbSa9OpM0udZtk/79DjBJLBvbkoK1e3jfFTw8VlqtVtq1EAAAAAAAAAAAAAAAAAAAAAAAAAAA4ACMH6bT6b/7uzlLdXKZqGdmn/npW65Vnh/MrCh/qgLhX076gkSVuWakb632cR3x1qfUiIdCmagqeIxyphRrhZeqjk/k+o5q9amQej2Uexxk9vQlEnL8sP1oRCzwoiTv/MVmcivxlFRftyhPD3GbANMnFXhT0HdW/MJCqW+MDPi7/JWFCgmX68xgTVkQmrBgwKuKaUBnQn0GjipDIZUjB7oBK5fkLpUjx5q+mpl28YtGoBZE+jUTTV6R6OtpBqw10hF9lGuYZin5NqHBUSX1Pos24BqM8IqhQG2EI376tFV6aPrUB38Cjy1c5JUTojhsPMJxSjIFjUdYxAOSKWjuw3FIsgqap5axtxwqv5MJbR4bEvjIwOp/Rf59ZGUlcOXfR1IrgfnMoD1rdh3jXJx3KfkYeXHivqVvPecGy3WLDtNx85GtwN0fCh5bPbbJ77Xe2a5OqdjdWoSeVplTW4GDrfnsTP+WM4/UVmC4tH/QlsQp0OkBqNU0wXU3HZtl+m/P6PICXUP4vEWBgetI3TSfNR08kFj80CTxlQg6CZS3ylsi2HXY70/0G7N294U9618qmK/t24mebSieFO97Ws5NbAWelK57hoLXEFNakOUcZO/FLgLJ1kGWkYR9LGa/m2G/H+S/o85Mr3CochL2WV0+L37PMS/uOMYEbycL7M9m2J9uycOcD3paCDmesPI/o+5wP+Xvcb8nYX/TxP6ujv9t57Fy2dB4vC/WJqHxwHm8cWdfs8C/6oN93YxQzCuP9DFu2KOkFAItqt96JEOsV5fVzUJ5RjMJjSswP6Y0AgPDtoKAzMfNqoBFRlbOr7tJ1SBfEpaiZ42V8rSV6EUT7svvaGv5S90QpW6/W9puiKZ+kiAjD9PllqvzlxHdnZrTduTsbar7/nQXifHXPz6ulOz9hFtXGP++urIns+udrOntHAkWVHbH9rnkUkHCuXu3UiGjDu3NaS7vHnfB/i0Bgv97FjZs31Qxnd3PBQAAAAAAAAAAAAAAAAAAAAAAAAAAoOI/yWXRCvNNhIAAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Турция' AND delete_ts IS NULL;
    -- Узбекистан (UZ)
    UPDATE hunttech_country SET
        country_eng_name = 'Uzbekistan',
        country_short_name = 'UZ',
        alpha3_code = 'UZB',
        numeric_code = '860',
        currency_code = 'UZS',
        capital = 'Ташкент',
        phone_code = 998,
        flag_url = 'https://flagcdn.com/w320/uz.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAACgBAMAAACPjaivAAAAG1BMVEUwgff///8whzjuFi72i5e+MGC+MjBzqfq51P31ArK5AAAB1klEQVR42u3aMXKDMBCF4aWSSm8Fda6QK+QKuUJqUUEJFRw7hVae2EYpkjDaeP7XaLwwk29kSWwYixBCnjBh2ibPvF11O7pQ0EseYivfqpoOr6x2wyWPXWrmW76DR9VeRGSs3Hd2Ovv7j4kGGjV/rt138gRq7QuWqBcHwLE6gRJtd2wxiYiEZW4BrE+gj0TVwfURvaotNK/hGz7tEHSS0fsSXBs9vp5mj1SA0U27VXkQl75qt8/t2q2gqo/Ved2X3MfuSUTiPmy5PCw+gNGKox1BtpFig/V6DCx9VekHu3btVmUN9nnxdXtefOuYvpbb7+IgwcZ8LeWxlDkH/9WTxP2z2H03474fDOySp1+EtydhKif17XhXbnjQlL5KvLRbt/s4zP0kIjKNWxIRmXRKB+UGU9hft/TFKilvoMHKvS3XpfEqvHuN5aDdKhM3ZOFqr1QHAw72+q2UW53p13fUS8h9VYr2T1J235WbEOd9E0II+Zu8O4+8Oo9/4JvziDoPQIAAAQIECBAgQIAAAQIECBAgQIAAAQIECBAgQIAAAQIE6BPo/icB/Orjt8AP55EX5wEIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAAB/iSfFlHgBf84RO8AAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Узбекистан' AND delete_ts IS NULL;
    -- Украина (UA)
    UPDATE hunttech_country SET
        country_eng_name = 'Ukraine',
        country_short_name = 'UA',
        alpha3_code = 'UKR',
        numeric_code = '804',
        currency_code = 'UAH',
        capital = 'Киев',
        phone_code = 380,
        flag_url = 'https://flagcdn.com/w320/ua.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVAgMAAABbIsF9AAAACVBMVEUAW7v/1QCAmF1B+3A0AAAARElEQVR42u3MMQEAAAgDoJW0pCktsU8IQAIAAAAAAAAAQMmWZcqEQqFQKBQKhUKhUCgUCoVCoVAoFAqFQqFQKBQK34YHXHs1Zm0258oAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Украина' AND delete_ts IS NULL;
    -- Уругвай (UY)
    UPDATE hunttech_country SET
        country_eng_name = 'Uruguay',
        country_short_name = 'UY',
        alpha3_code = 'URY',
        numeric_code = '858',
        currency_code = 'UYU',
        capital = 'Монтевидео',
        phone_code = 598,
        flag_url = 'https://flagcdn.com/w320/uy.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Уругвай' AND delete_ts IS NULL;
    -- Финляндия (FI)
    UPDATE hunttech_country SET
        country_eng_name = 'Finland',
        country_short_name = 'FI',
        alpha3_code = 'FIN',
        numeric_code = '246',
        currency_code = 'EUR',
        capital = 'Хельсинки',
        phone_code = 358,
        flag_url = 'https://flagcdn.com/w320/fi.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADEBAMAAAAc6OXjAAAAD1BMVEUAL2z///8/YpDG0N7j6O+EJQeKAAAAnElEQVR42u3aQQkAIBAAwatgA7GBDeyfygT3EBQVZhNMgI2y0Ii0Xg4VgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgL8B20I1B7ZThSRJkiRJkiRJkiRJ0o2enyp8M4CAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIB7m2OMP0b/fbGJAAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Финляндия' AND delete_ts IS NULL;
    -- Франция (FR)
    UPDATE hunttech_country SET
        country_eng_name = 'France',
        country_short_name = 'FR',
        alpha3_code = 'FRA',
        numeric_code = '250',
        currency_code = 'EUR',
        capital = 'Париж',
        phone_code = 33,
        flag_url = 'https://flagcdn.com/w320/fr.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAD1BMVEX///8AJlTOESbeYG5Wb42QNrxKAAAAqklEQVR42u3OQQ0AIAwEsEumAAsowb8qPFz2bBU0p/FSmNuIoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKDgYvADlEQBQhZTEtMAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Франция' AND delete_ts IS NULL;
    -- Черногория (ME)
    UPDATE hunttech_country SET
        country_eng_name = 'Montenegro',
        country_short_name = 'ME',
        alpha3_code = 'MNE',
        numeric_code = '499',
        currency_code = 'EUR',
        capital = 'Подгорица',
        phone_code = 382,
        flag_url = 'https://flagcdn.com/w320/me.png',
        flag_image = decode('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', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Черногория' AND delete_ts IS NULL;
    -- Чехия (CZ)
    UPDATE hunttech_country SET
        country_eng_name = 'Czech Republic',
        country_short_name = 'CZ',
        alpha3_code = 'CZE',
        numeric_code = '203',
        currency_code = 'CZK',
        capital = 'Прага',
        phone_code = 420,
        flag_url = 'https://flagcdn.com/w320/cz.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAALVBMVEURRX7XFBr////rio3i6e9COGSEKEO9GSZGbZoeT4WWrcepvNBYfaSHob7T3OcPf6OhAAADOklEQVR42s3WwW1UQRBF0d5QsiVYvBRIgQycRK0rBHJgzR68IwTHMBEQApEgWTZ4xv/PdP/urr4vgrO4UlU5fWav3D/BgeXjAxxYHunAcqIDfzzBgegMn4H3j3AgOcMXIDfDFyA3w1cgNsPybyc6EJrhfyA0wzdA5lF+C0RmeAYk/oZnQGKG50BghhdAXoaXQFyGl0Bchu+AtAzfA2EZbgBZGW4AWRluAVEZbgJJGW4DQRluA0EZ7gA5Ge4BMRnuAikZ7gIpGe4DIRleATIyvAZEZHgNiMjwKpCQ4XUgIMMbwPtfcGC5e4ADy086sHyjAxdneBu4OMMK4NoMa4BLM6wCrsywCrgywzrgwgwrgesyrAUuy7AWuCzDauCqDOuBizJsAK7JsAG4JsMW4JIMm4ArMmwDLsiwDbggw0ZgfoatwPQMm4HZGTYDy90fODA5wwPA8p0OTM3wCDA1w0PAzAyPARMzPAjMy/AgMC/Do8C0DA8DszI8DkzK8DgwKcMOYE6GPcCUDLuAGRl2ATMy7AMmZNgJnJ9hL3B6hr3A6Rl2A2dn2A+cnOEA4NwMBwDnZjgCODXDIcCZGY4BTsxwDHBihoOA5RMdOC3DYcDymw6clOE44KQMBwLnZDgSOCXDocAZGQ4FzshwLHBChoOB4zMcDbz7MnijgaHBG+xzsYEmNvDDVzgwxAa62EATGzgjwKHAEBvoYgNNbOCkAMcBQ2ygiw00sYHzAhwEDLGBLjbQxAZODXAEMMQGuthAExs4O8BuYIgNdLGBJjYwIcA+YIgNdLGBJjYwJ8AOYIgNdLGBJjYwLcCjwBAb6GIDTWxgZoCHgCE20MUGmtjA5ADbgSE20MUGmtjA/AAbgSE20MUGmtjAJQG2AENsoIsNNLGBqwKsBobYQBcbaGIDFwZYBwyxgS420MQGrg2wAhhiA11soIkNXB7gLWCIDXSxgSY2kBDgVWCIDXSxgSY2EBLgPjDEBrrYQBMbyAlwBxhiA11soIkNRAW4BQyxgS420MQG0gJ8BwyxgS420MQGAgM8B4bYQBcbaGIDmQG+AYbYQBcbaGIDsQG+AkNsoIsNNLGB5ACfgSE20Nk+/QVCJU3z8a60VwAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Чехия' AND delete_ts IS NULL;
    -- Швейцария (CH)
    UPDATE hunttech_country SET
        country_eng_name = 'Switzerland',
        country_short_name = 'CH',
        alpha3_code = 'CHE',
        numeric_code = '756',
        currency_code = 'CHF',
        capital = 'Берн',
        phone_code = 41,
        flag_url = 'https://flagcdn.com/w320/ch.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAAFAAQMAAAD3XjfpAAAABlBMVEX/AAD///9BHTQRAAAAT0lEQVRo3u3WsQkAIAwAwWzgSO6/jQMIESwFwUot7usb4CMkSZL0bTVnHQRBEARBEATBpZKbGgiCIAiCIHgbWlcQBEEQBEEQPIaSJEl61gB5R8eo7wNesQAAAABJRU5ErkJggg==', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Швейцария' AND delete_ts IS NULL;
    -- Швеция (SE)
    UPDATE hunttech_country SET
        country_eng_name = 'Sweden',
        country_short_name = 'SE',
        alpha3_code = 'SWE',
        numeric_code = '752',
        currency_code = 'SEK',
        capital = 'Стокгольм',
        phone_code = 46,
        flag_url = 'https://flagcdn.com/w320/se.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADIAQMAAACjyqroAAAABlBMVEX+ywAAUpP1seVEAAAAP0lEQVRo3u3VoQ0AIBAEweuA/rukgweJeEsCyawev6mjmd2otoAgCIIgCIIgCD4GJUm6mQ2DIAiCIAiCIPgpXIqpy1zXbyk6AAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Швеция' AND delete_ts IS NULL;
    -- Эстония (EE)
    UPDATE hunttech_country SET
        country_eng_name = 'Estonia',
        country_short_name = 'EE',
        alpha3_code = 'EST',
        numeric_code = '233',
        currency_code = 'EUR',
        capital = 'Таллин',
        phone_code = 372,
        flag_url = 'https://flagcdn.com/w320/ee.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADMAgMAAAB/+5IuAAAACVBMVEX///8Acs4AAABr0YPHAAAAS0lEQVR42u3MMQEAAAQAMCWVlNJHAb4twCKPhVAoFAqFQqFQKBQKhUKhUCgUCiesY0KhUCgUCoVCoVAoFAqFQqFQKNwQAAAAAIA3DXj7K/zG3UvgAAAAAElFTkSuQmCC', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Эстония' AND delete_ts IS NULL;
    -- Япония (JP)
    UPDATE hunttech_country SET
        country_eng_name = 'Japan',
        country_short_name = 'JP',
        alpha3_code = 'JPN',
        numeric_code = '392',
        currency_code = 'JPY',
        capital = 'Токио',
        phone_code = 81,
        flag_url = 'https://flagcdn.com/w320/jp.png',
        flag_image = decode('iVBORw0KGgoAAAANSUhEUgAAAUAAAADVBAMAAADUYjTdAAAAIVBMVEW8AC3////DG0PuvMj78fPMPV/12d/mn7DUW3fbd47hi5/TxgOgAAADPklEQVR42u3cTU9TQRQG4OOtpR8rh1ta7KpENLprhUSXFhKjrooLorvWaCI7blyQsCpBXReDuC0rI7/SRHGhKe2dOZ1zjuV9f0DzJHNnOh9nhpzxEIAAAggggAACCCCAAAIIIIAAAggggAACCCCAAAIIIIDXALj96ODi4uDDpk1g+uMlXebut4454PZX+iuvN00B/+XNkTgXYDqkCbnfsQLca9HEJGc2gHt0Zc4sAN/TlHzWBz6jqfmoDWy2pgMp0wWmvRk+KnZUgSOamYom8B3lyK4esEm5kmkBa8N8wHJXCfiGcuaeDrDeygtMBirANuVOQQPYJI9kCsCRD7AiD2yQV87FgW0/YEEamJJnOsLAvi9wSRZYa/kCk64ocJW8cyQKHPsDq5LAlAIyEARuhQDXBYGjEGBFDlgnEmvjIOBKGHBHDLgfBixJAf1HacZYHQK8TYE5EQJuhQLXhYDjUGBVBhj6CYZ9hAHAJgUnEwEuhwPXRIDtcGBBBNgLB5YlgDVipCsAbHCAmQBwhQPcEQD2OcCbAsAxB1gSAA45wLIAkOOjJD6wzgL6T/u9gU0eMIsOXOUBj6IDl3nAtejAWzzgjejAPg+4FB3Y5gEL0YFjHrAaHTjkAcvRgT0esAigeWCLZGcLiwckZgA0D0QnwUAN4P8+mzE/HzQ/oza/JjG/qjO/Lja/s9DgAePvzZjf3eLNFgT2B+3vsO5zgBJ71E+s7/KbPycxf9Jk/qyO041FTjs53VjmvNj8ibv5mgXzVR/m62bsVx6Zr90yX/1mvn7QfgVmGgYciAHNVwHbr6Oui7VwaC3/vlAfXtzbEObvk+S/lfgnobcTF/ZOk/lbYb6rz0wc6PdvUnXyQK+lybkC0GewLjkNYBq/C/OA7mHsMZALrOc8tysOlIB5V08nTguYr5HvOD1gLUcjF7uKQNeYOatJMqcJdM9nAU+dLnDWcx+7ThvoHk/zHTt94LQ3hfgvCs3l1agXV/SU5NTZALr0yyTfg46zApz4IR7P55evydtvv9r58NPlt5i8Mvh63u883Tj8vvF2rj+JJyYBBBBAAAEEEEAAAQQQQAABBBBAAAEEEEAAAQQQQAABDMtPOpFrHrlvUBIAAAAASUVORK5CYII=', 'base64'),
        version = version + 1,
        update_ts = now()
    WHERE country_ru_name = 'Япония' AND delete_ts IS NULL;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    RAISE NOTICE 'Обновлено стран: %', v_count;
END $$;