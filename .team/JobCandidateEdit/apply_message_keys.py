#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Добавление недостающих ключей компоновки JobCandidateEdit (дизайн 2.1.8/2.4) в локальный пакет."""
import io, sys

BASE = 'modules/web/src/com/company/hunttech/web/screens/jobcandidate/messages'

RU = """# Ключи компоновки формы (дизайн-ревью 2026-08-03)
msgAdditionalContacts=Дополнительные контакты
msgAdditionalPositions=Доп. позиции
msgCancel=Отмена
msgCard=Карточка
msgCreateCV=Создать резюме
msgCreateIteraction=Создать взаимодействие
msgCreateTsLabel=Дата создания
msgCreatedByUser=Создал
msgCreatedUpdated=Создано/Изменено
msgFormSections=Разделы формы
msgIteractionType=Тип взаимодействия
msgMainContacts=Основные контакты
msgMobile=Мобильный
msgMore=Еще
msgNetworkName=Социальная сеть
msgPersonalData=Персональные данные
msgProfessionalData=Профессиональные данные
msgRecordData=Данные записи
msgSaveAndClose=Сохранить и закрыть
msgSocialNetworks=Социальные сети
msgTabHistory=История
msgTabMain=Основное
msgTabPositions=Позиции и вакансии
msgTabResume=Резюме и файлы
"""

EN = """# Screen layout keys (design review 2026-08-03)
msgAdditionalContacts=Additional contacts
msgAdditionalPositions=Additional positions
msgCancel=Cancel
msgCard=Card
msgCreateCV=Create CV
msgCreateIteraction=Create interaction
msgCreateTsLabel=Creation date
msgCreatedByUser=Created by
msgCreatedUpdated=Created/Updated
msgFormSections=Form sections
msgIteractionType=Interaction type
msgMainContacts=Main contacts
msgMobile=Mobile
msgMore=More
msgNetworkName=Social network
msgPersonalData=Personal data
msgProfessionalData=Professional data
msgRecordData=Record data
msgSaveAndClose=Save and Close
msgSocialNetworks=Social networks
msgTabHistory=History
msgTabMain=Main
msgTabPositions=Positions and vacancies
msgTabResume=Resume and files
"""

def add_keys(path, block):
    with io.open(path, encoding='utf-8') as f:
        s = f.read()
    for line in block.splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        key = line.split('=')[0]
        if ('\n' + key + '=') in s or s.startswith(key + '='):
            print('WARN: ключ уже существует:', key)
    if not s.endswith('\n'):
        s += '\n'
    s += '\n' + block
    with io.open(path, 'w', encoding='utf-8') as f:
        f.write(s)
    print('OK:', path)

add_keys(BASE + '.properties', EN)
add_keys(BASE + '_ru.properties', RU)
