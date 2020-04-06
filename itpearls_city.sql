--
-- PostgreSQL database dump
--

-- Dumped from database version 11.7 (Ubuntu 11.7-2.pgdg18.04+1)
-- Dumped by pg_dump version 11.7

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: itpearls_city; Type: TABLE DATA; Schema: public; Owner: cuba
--

INSERT INTO public.itpearls_city VALUES ('fa3835f0-e74b-7b20-3938-e35ca92b36f5', 1, '2019-10-22 18:57:56.57', 'admin', '2019-10-22 18:57:56.57', NULL, NULL, NULL, 'Архангельск', NULL, '8960e3a5-de9e-fc46-c622-fa9b82672ebb');
INSERT INTO public.itpearls_city VALUES ('af70f94f-b59d-8b34-3228-f5f1bad9970b', 1, '2019-10-22 18:57:56.647', 'admin', '2019-10-22 18:57:56.647', NULL, NULL, NULL, 'Балаково', NULL, '44493fed-0203-b064-3f11-60392045d3ab');
INSERT INTO public.itpearls_city VALUES ('6cc5b090-f5e1-bbf9-d35c-3b0208f9054a', 1, '2019-10-22 18:57:56.691', 'admin', '2019-10-22 18:57:56.691', NULL, NULL, NULL, 'Барнаул', NULL, 'ac848860-e91a-5352-d5fb-1960d969d0fc');
INSERT INTO public.itpearls_city VALUES ('8e4ebef6-57e9-09a2-f545-35bd1a8f9519', 1, '2019-10-22 18:57:56.825', 'admin', '2019-10-22 18:57:56.825', NULL, NULL, NULL, 'Вильнюс', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('53387d19-124d-ed31-e35c-95eef466a239', 1, '2019-10-22 18:57:56.891', 'admin', '2019-10-22 18:57:56.891', NULL, NULL, NULL, 'Владивосток', NULL, 'ce094f5c-382b-ca2b-488b-a58f59c10380');
INSERT INTO public.itpearls_city VALUES ('8a6e57cb-7ad5-6924-a2f4-5d38f88d7065', 1, '2019-10-22 18:57:56.937', 'admin', '2019-10-22 18:57:56.937', NULL, NULL, NULL, 'Владимир', NULL, '7f475ed4-4b51-9617-7fd8-2148c30a5437');
INSERT INTO public.itpearls_city VALUES ('5a058b6a-5dc4-a132-10b3-32e55b811260', 1, '2019-10-22 18:57:57.006', 'admin', '2019-10-22 18:57:57.006', NULL, NULL, NULL, 'Волгоград', NULL, 'a5d23938-c2d5-46e1-fc6e-afca460e617f');
INSERT INTO public.itpearls_city VALUES ('72a06c34-5a3c-280f-7d76-ba3f34ba1999', 1, '2019-10-22 18:57:58.322', 'admin', '2019-10-22 18:57:58.322', NULL, NULL, NULL, 'Воронеж', NULL, '5dee8fb5-1fad-b1ad-3153-ca2e476a93ef');
INSERT INTO public.itpearls_city VALUES ('a64553d3-854c-fd60-8f93-c6ab38f92d8a', 1, '2019-10-22 18:57:58.481', 'admin', '2019-10-22 18:57:58.481', NULL, NULL, NULL, 'Иваново', NULL, '5f537297-9132-dace-be41-685a2b213683');
INSERT INTO public.itpearls_city VALUES ('cbff2945-80ec-94c2-8ef1-77b9af971642', 1, '2019-10-22 18:57:58.576', 'admin', '2019-10-22 18:57:58.576', NULL, NULL, NULL, 'Иннополис', NULL, '7c0a05c6-6b40-4f09-7365-c30990a5dea7');
INSERT INTO public.itpearls_city VALUES ('bf5758a2-aa84-a3bd-fb7f-17512e0770ef', 1, '2019-10-22 18:57:58.66', 'admin', '2019-10-22 18:57:58.66', NULL, NULL, NULL, 'Казань', NULL, '7c0a05c6-6b40-4f09-7365-c30990a5dea7');
INSERT INTO public.itpearls_city VALUES ('2d9f93e8-96d0-53f7-6484-12601b020e18', 1, '2019-10-22 18:57:59.454', 'admin', '2019-10-22 18:57:59.454', NULL, NULL, NULL, 'Новотроицк', NULL, '3d7d7a10-0a35-0dcb-73ce-989fcac9fc6f');
INSERT INTO public.itpearls_city VALUES ('48b30f16-5d96-da18-e96d-5032e6f0ad31', 1, '2019-10-22 18:58:00.201', 'admin', '2019-10-22 18:58:00.201', NULL, NULL, NULL, 'Таганрог', NULL, 'cc6514a5-d306-b171-7917-dbc68552f34a');
INSERT INTO public.itpearls_city VALUES ('a25a93c1-d1ed-a432-02b6-05c4044febdd', 1, '2019-10-22 18:58:00.979', 'admin', '2019-10-22 18:58:00.979', NULL, NULL, NULL, 'Энгельс', NULL, '44493fed-0203-b064-3f11-60392045d3ab');
INSERT INTO public.itpearls_city VALUES ('1acceae8-fc57-3d45-369a-a2e1f58920b4', 1, '2019-11-07 10:49:28.713', 'nmironova', '2019-11-07 10:49:28.713', NULL, NULL, NULL, 'Ставрополь', '8652', '3d175505-d389-c9ca-c4fa-7aca339cf5f6');
INSERT INTO public.itpearls_city VALUES ('276b3aae-627e-9bd0-4695-12326b6ee946', 2, '2019-10-22 18:57:58.968', 'admin', '2019-11-14 22:47:49.793', 'alan', NULL, NULL, 'Москва', NULL, 'e2a4ff8f-7060-e2e9-a86a-93c141674a52');
INSERT INTO public.itpearls_city VALUES ('5da62932-b14f-be9b-4f86-476bac0a92f6', 2, '2019-10-22 18:57:58.615', 'admin', '2019-11-14 22:48:08.084', 'alan', NULL, NULL, 'Ирландия', NULL, '1d95a777-62e1-ef24-1ad7-26c77939008d');
INSERT INTO public.itpearls_city VALUES ('10080ace-54f9-eaaa-063b-447fb64bca78', 2, '2019-10-22 18:57:56.471', 'admin', '2019-11-14 22:48:38.36', 'alan', NULL, NULL, 'Алма-Ата', NULL, '6a990d63-9e4b-a8ee-d35f-4b785020611a');
INSERT INTO public.itpearls_city VALUES ('f7577ccb-7623-8ea4-79b9-bbba1ccf625e', 2, '2019-10-22 18:57:56.52', 'admin', '2019-11-14 22:48:46.31', 'alan', NULL, NULL, 'Армения', NULL, '1134146f-ac31-15a6-72cf-fe69b3cc5c97');
INSERT INTO public.itpearls_city VALUES ('6595f545-9096-2ac2-677b-84fb1534c242', 2, '2019-10-22 18:57:56.601', 'admin', '2019-11-14 22:48:56.355', 'alan', NULL, NULL, 'Астана', NULL, '6a990d63-9e4b-a8ee-d35f-4b785020611a');
INSERT INTO public.itpearls_city VALUES ('c55f4a5a-7a82-3fb7-12fa-a214035adb41', 2, '2019-10-22 18:57:56.729', 'admin', '2019-11-14 22:49:06.827', 'alan', NULL, NULL, 'Берлин', NULL, 'e6a452f1-40d8-5aad-0cbc-b18d937c5bac');
INSERT INTO public.itpearls_city VALUES ('c4849940-f9e9-32de-9231-206fc830cf79', 2, '2019-10-22 18:57:56.762', 'admin', '2019-11-14 22:49:50.176', 'alan', NULL, NULL, 'Бишкек', NULL, '74f79ecd-4505-c965-9499-3e481d18b3d0');
INSERT INTO public.itpearls_city VALUES ('6b42555b-29a0-7051-29b0-fef07ad4aa38', 2, '2019-10-22 18:57:56.796', 'admin', '2019-11-14 22:50:00.478', 'alan', NULL, NULL, 'Болгария', NULL, 'b571bff1-30f2-0153-c01b-f3480773243f');
INSERT INTO public.itpearls_city VALUES ('df088b61-7c17-89e4-4f12-dedac3a55939', 2, '2019-10-22 18:58:00.035', 'admin', '2019-11-14 22:51:10.884', 'alan', NULL, NULL, 'Саранск', NULL, '6cd46d2e-9323-47ed-7b73-a785c9659cf8');
INSERT INTO public.itpearls_city VALUES ('c5383768-07f8-4131-dd0e-73154321aade', 2, '2019-10-22 18:58:00.117', 'admin', '2019-11-14 22:51:19.469', 'alan', NULL, NULL, 'Свердловск', NULL, '12adcc85-fa1c-6899-1838-c31e273a982e');
INSERT INTO public.itpearls_city VALUES ('af184cb9-2005-f678-b86b-5e7c113f120b', 2, '2019-10-22 18:57:59.886', 'admin', '2019-11-14 22:51:29.08', 'alan', NULL, NULL, 'Самара', NULL, '048a655d-4e8c-daa4-26ce-76ee59dbbdc1');
INSERT INTO public.itpearls_city VALUES ('a1bbca7e-6ceb-9aba-6551-32ae1bfd35ba', 2, '2019-10-22 18:57:59.829', 'admin', '2019-11-14 22:51:38.449', 'alan', NULL, NULL, 'Рязань', NULL, '0225558e-bacd-fd89-1a05-3c725d34f7f8');
INSERT INTO public.itpearls_city VALUES ('36815bf9-b26e-e002-4335-603c1b339c83', 2, '2019-10-22 18:57:58.377', 'admin', '2019-11-14 22:52:01.9', 'alan', NULL, NULL, 'Донецк', NULL, 'ca4af733-3f33-3fd6-6fd4-b679aecea65e');
INSERT INTO public.itpearls_city VALUES ('a7866d77-30fe-a778-dfe4-b2f16a5606d9', 2, '2019-10-22 18:57:58.438', 'admin', '2019-11-14 22:52:10.302', 'alan', NULL, NULL, 'Екатеринбург', NULL, '1ad5d7a7-0868-afd6-e83f-de3cf99fdee7');
INSERT INTO public.itpearls_city VALUES ('b5b66136-5f64-e6f5-b161-e1f9d8cf47c3', 2, '2019-10-22 18:57:58.708', 'admin', '2019-11-14 22:52:26.449', 'alan', NULL, NULL, 'Кемерово', NULL, '1ad5d7a7-0868-afd6-e83f-de3cf99fdee7');
INSERT INTO public.itpearls_city VALUES ('89781234-0239-c93e-fe35-82959c83bb3b', 2, '2019-10-22 18:57:58.748', 'admin', '2019-11-14 22:53:17.746', 'alan', NULL, NULL, 'Киров', NULL, '06e723d4-d2f7-f1ad-11ae-90d7442eef98');
INSERT INTO public.itpearls_city VALUES ('04ebc32a-fcc1-d368-31cb-425143893429', 2, '2019-10-22 18:57:58.787', 'admin', '2019-11-14 22:53:30.546', 'alan', NULL, NULL, 'Краснодар', NULL, '21881497-f3f2-df42-cafe-a6b35b66d897');
INSERT INTO public.itpearls_city VALUES ('9b107d0d-7605-fa2c-fc32-6c1f333e0972', 2, '2019-10-22 18:57:58.822', 'admin', '2019-11-14 22:53:39.538', 'alan', NULL, NULL, 'Красноярск', NULL, 'd2579465-90d7-2b3c-b0ee-3c69d5e8dfc7');
INSERT INTO public.itpearls_city VALUES ('ed6dbe44-7f0d-9c11-b1a1-bc179e37d230', 2, '2019-10-22 18:57:58.878', 'admin', '2019-11-14 22:54:27.516', 'alan', NULL, NULL, 'Минск', NULL, '64e01967-af91-2a65-b32f-2918815ad58d');
INSERT INTO public.itpearls_city VALUES ('83421236-502b-d440-12b0-8ad85e66cd39', 2, '2019-10-22 18:57:59.232', 'admin', '2019-11-14 22:54:54.439', 'alan', NULL, NULL, 'Мурманск', NULL, '5bfcc63e-6240-b842-dcff-b95158145bf3');
INSERT INTO public.itpearls_city VALUES ('3f37c32b-1209-4be5-3ac7-2c149a3ade00', 2, '2019-10-22 18:57:59.291', 'admin', '2019-11-14 22:55:03.984', 'alan', NULL, NULL, 'Нижний Новгород', NULL, '64a01417-d649-151d-c7d4-f30fc08ccaa6');
INSERT INTO public.itpearls_city VALUES ('38a2d1f0-7239-4f13-12b2-8fb61356d6a6', 2, '2019-10-22 18:57:59.345', 'admin', '2019-11-14 22:55:12.927', 'alan', NULL, NULL, 'Новороссийск', NULL, '21881497-f3f2-df42-cafe-a6b35b66d897');
INSERT INTO public.itpearls_city VALUES ('2032d0fd-eda0-629f-c7a7-883532a45869', 2, '2019-10-22 18:57:59.395', 'admin', '2019-11-14 22:55:21.927', 'alan', NULL, NULL, 'Новосибирск', NULL, 'af43525a-5043-242d-72e8-5363c372582b');
INSERT INTO public.itpearls_city VALUES ('3d6d5c84-bbbe-5263-adb0-71f5da758735', 2, '2019-10-22 18:57:59.512', 'admin', '2019-11-14 22:55:37.323', 'alan', NULL, NULL, 'Омск', NULL, '9db5d632-aa3e-8d8f-206c-a8a034d230b3');
INSERT INTO public.itpearls_city VALUES ('a10ebff3-7896-fae8-04e8-08849a778976', 2, '2019-10-22 18:57:59.567', 'admin', '2019-11-14 22:55:51.735', 'alan', NULL, NULL, 'Оренбург', NULL, '3d7d7a10-0a35-0dcb-73ce-989fcac9fc6f');
INSERT INTO public.itpearls_city VALUES ('6eaf9b15-ff2a-0483-7ac2-982309b7dbc7', 2, '2019-10-22 18:57:59.608', 'admin', '2019-11-14 22:55:59.185', 'alan', NULL, NULL, 'Пенза', NULL, '593e6c20-0346-d680-f832-eaf8c597f070');
INSERT INTO public.itpearls_city VALUES ('bb073cc9-9f9a-5390-3b7c-3797fe7a3204', 2, '2019-10-22 18:57:59.652', 'admin', '2019-11-14 22:56:06.944', 'alan', NULL, NULL, 'Пермь', NULL, '0855a8bf-5c3d-6c32-6c8e-946f835c9c8f');
INSERT INTO public.itpearls_city VALUES ('035a9da8-736b-8ebe-ef5f-501476303b67', 2, '2019-10-22 18:57:59.774', 'admin', '2019-11-14 22:57:07.415', 'alan', NULL, NULL, 'Ростов-на-Дону', NULL, '34fe9760-6cd3-f605-43b4-97903263e43a');
INSERT INTO public.itpearls_city VALUES ('4336902d-bdb8-4ebf-e491-7f0b4995a3da', 3, '2019-10-22 18:57:59.715', 'admin', '2019-11-14 22:57:21.423', 'alan', NULL, NULL, 'Ростов', NULL, '259dd790-8aa3-99d1-3fc3-f09b6b47f66d');
INSERT INTO public.itpearls_city VALUES ('6f893135-5b60-0c60-6f33-793e1142fcf6', 2, '2019-10-22 18:57:59.995', 'admin', '2019-11-14 22:57:32.893', 'alan', NULL, NULL, 'Санкт-Петербург', NULL, '9d986a8f-d1a4-3eb3-0fdb-0670386adb10');
INSERT INTO public.itpearls_city VALUES ('0000ad80-2dcd-01f9-762e-1469fa6219b4', 2, '2019-10-22 18:57:59.937', 'admin', '2019-11-14 22:58:14.365', 'alan', NULL, NULL, 'Сан-Франциско', NULL, '87d1c98e-5719-bbe2-db20-ab3e2c77fa0a');
INSERT INTO public.itpearls_city VALUES ('34562e12-b659-77f2-dbde-2e3f4e61a652', 2, '2019-10-22 18:58:00.082', 'admin', '2019-11-14 22:58:27.249', 'alan', NULL, NULL, 'Саратов', NULL, '44493fed-0203-b064-3f11-60392045d3ab');
INSERT INTO public.itpearls_city VALUES ('2c3b8a91-8bc3-93bf-1d67-cd64497c36e0', 2, '2019-10-22 18:58:00.154', 'admin', '2019-11-14 22:58:38.02', 'alan', NULL, NULL, 'Сочи', NULL, '21881497-f3f2-df42-cafe-a6b35b66d897');
INSERT INTO public.itpearls_city VALUES ('ef0b44af-2d71-fecc-586c-7f6b5612f675', 2, '2019-10-22 18:58:00.242', 'admin', '2019-11-14 22:59:30.354', 'alan', NULL, NULL, 'Ташкент', NULL, '8c78d2e5-c260-de33-c0d3-c17088ce4151');
INSERT INTO public.itpearls_city VALUES ('4fb5e534-19f2-f7cd-d306-5bda98196260', 2, '2019-11-14 19:11:52.01', 'alan', '2019-11-14 22:59:59.305', 'alan', NULL, NULL, 'Тель-Авив', NULL, 'cdf0579b-45ce-251b-750b-3f74615dea65');
INSERT INTO public.itpearls_city VALUES ('98880366-2493-3844-e0c3-8d7c14ac09c7', 2, '2019-10-22 18:58:00.335', 'admin', '2019-11-14 23:00:17.428', 'alan', NULL, NULL, 'Томск', NULL, 'cc6514a5-d306-b171-7917-dbc68552f34a');
INSERT INTO public.itpearls_city VALUES ('5b53e65d-e8a8-dc7f-57f5-c08ab737b3b8', 2, '2019-10-22 18:58:00.468', 'admin', '2019-11-14 23:00:27.706', 'alan', NULL, NULL, 'Тула', NULL, '6b345d85-2004-8b07-033a-05cf02a959f2');
INSERT INTO public.itpearls_city VALUES ('a5d9642f-9997-a3ea-5931-e1ebb82305d7', 2, '2019-10-22 18:58:00.51', 'admin', '2019-11-14 23:00:37.009', 'alan', NULL, NULL, 'Тюмень', NULL, '9aa49151-a774-0e5f-fc40-a08fb2e46055');
INSERT INTO public.itpearls_city VALUES ('3ddd09d6-d532-d3cf-2238-b7a380b6d238', 2, '2019-10-22 18:58:00.549', 'admin', '2019-11-14 23:00:43.767', 'alan', NULL, NULL, 'Ульяновск', NULL, '871f73f4-78ee-6803-7862-e84e34ea758b');
INSERT INTO public.itpearls_city VALUES ('d65ccb2b-d5c8-d094-8880-57571a43893b', 2, '2019-10-22 18:58:00.595', 'admin', '2019-11-14 23:00:58.931', 'alan', NULL, NULL, 'Уфа', NULL, 'ddefb875-4312-5347-02e2-935438c25f60');
INSERT INTO public.itpearls_city VALUES ('c34d082c-e429-d45e-68ee-aeccd789ded1', 2, '2019-10-22 18:58:00.633', 'admin', '2019-11-14 23:01:15.827', 'alan', NULL, NULL, 'Харьков', NULL, 'd90a8426-4615-3152-e987-a0fba0693939');
INSERT INTO public.itpearls_city VALUES ('5394d186-48d5-1cda-5bb1-fa23dda6a18b', 2, '2019-10-22 18:58:00.68', 'admin', '2019-11-14 23:01:45.113', 'alan', NULL, NULL, 'Чебоксары', NULL, '27fe2de8-92ac-049e-a8e7-116584b293c1');
INSERT INTO public.itpearls_city VALUES ('213c31b7-cf06-cff9-ad76-963bffa86436', 2, '2019-10-22 18:58:00.72', 'admin', '2019-11-14 23:01:53.711', 'alan', NULL, NULL, 'Челябинск', NULL, '12728732-02e2-33b8-0e2c-c890f5bb7aa7');
INSERT INTO public.itpearls_city VALUES ('2b3d3a5a-dd98-187d-9e96-dcb9d4ebdf01', 2, '2019-10-22 18:58:00.758', 'admin', '2019-11-14 23:02:16.176', 'alan', NULL, NULL, 'Череповец', NULL, '6c63225a-7759-80fa-9e4f-e005c6f06467');
INSERT INTO public.itpearls_city VALUES ('8edbb601-6887-ce9e-294e-412e5239489a', 2, '2019-10-22 18:58:00.934', 'admin', '2019-11-14 23:03:03.602', 'alan', NULL, NULL, 'Шанхай', NULL, 'fa294492-9963-9882-78b7-49de03364293');
INSERT INTO public.itpearls_city VALUES ('f8ea437e-4cef-a489-b297-5b77360b8a09', 2, '2019-10-22 18:58:01.021', 'admin', '2019-11-14 23:03:13.65', 'alan', NULL, NULL, 'Ярославль', NULL, '259dd790-8aa3-99d1-3fc3-f09b6b47f66d');
INSERT INTO public.itpearls_city VALUES ('27d3ef31-8bc8-469e-edbe-355e20dca155', 2, '2019-10-22 18:58:01.052', 'admin', '2019-11-14 23:27:57.104', 'alan', NULL, NULL, 'Herrsching am Ammersee', NULL, '87d1c98e-5719-bbe2-db20-ab3e2c77fa0a');
INSERT INTO public.itpearls_city VALUES ('7a2b2556-87d8-d4e0-e7f7-76cdbc8e1324', 2, '2019-10-22 18:58:01.111', 'admin', '2019-11-14 23:28:10.348', 'alan', NULL, NULL, 'San Francisco', NULL, '87d1c98e-5719-bbe2-db20-ab3e2c77fa0a');
INSERT INTO public.itpearls_city VALUES ('c93a7a21-b019-86e2-b13e-1a3abea75089', 2, '2019-10-22 18:57:59.023', 'admin', '2019-11-14 22:54:43.736', 'alan', NULL, NULL, 'Московская область', NULL, 'd6c2c881-97bb-3fbe-6311-7d55775620a1');
INSERT INTO public.itpearls_city VALUES ('e1c0abbe-b75f-c73f-58da-9df39e3271b6', 2, '2019-10-22 18:58:00.29', 'admin', '2019-11-14 23:00:10.198', 'alan', NULL, NULL, 'Тольятти', NULL, '048a655d-4e8c-daa4-26ce-76ee59dbbdc1');
INSERT INTO public.itpearls_city VALUES ('ce541eba-a712-6bd0-0117-1fbaaeb1421d', 2, '2019-10-22 18:58:01.074', 'admin', '2019-11-14 23:28:03.57', 'alan', NULL, NULL, 'Pleasanton', NULL, '87d1c98e-5719-bbe2-db20-ab3e2c77fa0a');
INSERT INTO public.itpearls_city VALUES ('e2203d5a-9c03-f3e5-3618-14e7c6ce9040', 1, '2019-11-18 12:21:51.495', 'alan', '2019-11-18 12:21:51.495', NULL, NULL, NULL, 'Благовещенск', '4162', 'e9138639-2c34-831c-ccaa-622c50208639');
INSERT INTO public.itpearls_city VALUES ('c882bbed-dda2-a011-a5f5-aa4bb7c391a5', 1, '2019-11-28 19:20:12.375', 'alan', '2019-11-28 19:20:12.375', NULL, NULL, NULL, 'Сургут', NULL, '91de3fb8-fecf-cf3f-590a-6addc4668874');
INSERT INTO public.itpearls_city VALUES ('e12bca64-2633-f1e0-3b1b-fdb597e67b61', 1, '2019-12-02 20:57:54.095', 'tav', '2019-12-02 20:57:54.095', NULL, NULL, NULL, 'Иркутск', NULL, '237f8521-dcfc-9119-c42b-f132d79dbc70');
INSERT INTO public.itpearls_city VALUES ('e314e1eb-3c07-2b2c-85af-96aba71aae48', 1, '2019-12-09 17:20:16.508', 'tav', '2019-12-09 17:20:16.508', NULL, NULL, NULL, 'Липецк', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('9e5c046e-6a3f-714b-1d46-1584106b460a', 1, '2019-12-10 18:41:54.579', 'alan', '2019-12-10 18:41:54.579', NULL, NULL, NULL, 'Ужевск', '3412', '65d02461-e640-9aa4-a099-51d34c4b3778');
INSERT INTO public.itpearls_city VALUES ('d7ae8a9f-566d-f214-a7c8-4fe655d71b1f', 1, '2019-12-21 10:37:50.562', 'alan', '2019-12-21 10:37:50.562', NULL, NULL, NULL, 'Висконсин', NULL, '939ff69d-b0b2-89a4-fd1d-8eab312d96cf');
INSERT INTO public.itpearls_city VALUES ('3ab154f3-705e-a751-ead7-9edbc7fd5420', 1, '2020-01-21 22:15:17.455', 'alan', '2020-01-21 22:15:17.455', NULL, NULL, NULL, 'Калининград', '4012', '0330e313-85c0-d3b0-8a4d-cca750f2852d');
INSERT INTO public.itpearls_city VALUES ('efb4e8b7-773c-a809-2f55-1d2061528ce8', 1, '2020-02-06 17:28:37.957', 'obudischeva', '2020-02-06 17:28:37.957', NULL, NULL, NULL, 'Ижевск', NULL, '65d02461-e640-9aa4-a099-51d34c4b3778');
INSERT INTO public.itpearls_city VALUES ('7c45722b-509f-1062-677b-b052ccda8679', 1, '2020-02-11 16:10:31.988', 'obudischeva', '2020-02-11 16:10:31.988', NULL, NULL, NULL, 'Симферополь', NULL, 'd4e0d71e-0e1f-2b4f-ba6f-41733aa42cc6');
INSERT INTO public.itpearls_city VALUES ('29cfaf19-cda5-f441-ec63-0d7b16b14d7a', 1, '2020-02-25 10:47:29.011', 'ddt', '2020-02-25 10:47:29.011', NULL, NULL, NULL, 'Норильск', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('096e1faf-ad8b-00ec-5b09-fd99a38ed2a8', 1, '2020-02-27 15:05:44.715', 'ddt', '2020-02-27 15:05:44.715', NULL, NULL, NULL, 'Питтсбург', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('140ae30e-da2b-9d85-64bd-b2c881c4cc6a', 1, '2020-03-04 09:23:29.964', 'ddt', '2020-03-04 09:23:29.964', NULL, NULL, NULL, 'Сидней', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('e9df85e0-94c1-6e2f-4db8-f921ade4d8e9', 1, '2020-03-04 09:33:02.774', 'ddt', '2020-03-04 09:33:02.774', NULL, NULL, NULL, 'Бока-Ратон', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('419cd2e4-8c93-8270-db27-72fbc45152ac', 1, '2020-03-04 09:46:37.73', 'ddt', '2020-03-04 09:46:37.73', NULL, NULL, NULL, 'Сингапур', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('9de9acfc-6023-e727-0316-653a4eed3c3a', 1, '2020-03-04 11:11:59.69', 'ddt', '2020-03-04 11:11:59.69', NULL, NULL, NULL, 'Yeonsu-gu', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('62ca9ae2-ec94-0274-a0cb-4a1396f81bad', 1, '2020-03-04 11:13:20.959', 'ddt', '2020-03-04 11:13:20.959', NULL, NULL, NULL, 'Инчхон', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('168b8390-4db2-58d6-2105-23e47c1aae2b', 1, '2020-03-04 11:20:30.488', 'ddt', '2020-03-04 11:20:30.488', NULL, NULL, NULL, 'Денвер', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('1d6678f4-ea15-f4c3-bd48-22c218616698', 1, '2020-03-06 10:06:05.715', 'obudischeva', '2020-03-06 10:06:05.715', NULL, NULL, NULL, 'Хабаровск', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('40f3fe17-1081-830f-9d88-1851555ef7ef', 1, '2020-03-17 19:00:31.3', 'akuzmich', '2020-03-17 19:00:31.3', NULL, NULL, NULL, 'Туапсе', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('1716e8e5-9587-ddf6-a48c-def5720d3e16', 1, '2020-03-17 19:30:24.853', 'akuzmich', '2020-03-17 19:30:24.853', NULL, NULL, NULL, 'Сыктывкар', NULL, NULL);
INSERT INTO public.itpearls_city VALUES ('5a09566f-e8b9-6203-de5d-4083b66aaf8c', 2, '2020-03-31 09:57:30.135', 'ddt', '2020-03-31 09:58:47.692', 'ddt', NULL, NULL, 'Sunnyvale', NULL, '87d1c98e-5719-bbe2-db20-ab3e2c77fa0a');
INSERT INTO public.itpearls_city VALUES ('15417f06-e330-213d-b009-418b5ccdedfd', 1, '2020-03-31 10:25:17.475', 'ddt', '2020-03-31 10:25:17.475', NULL, NULL, NULL, 'Portland', NULL, '87d1c98e-5719-bbe2-db20-ab3e2c77fa0a');


--
-- PostgreSQL database dump complete
--

