#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Presentation-only правки job-candidate-edit.xml по дизайн-ревью (раздел 2.1).
Каждая замена: точная строка + ожидаемое число вхождений. При несовпадении — abort."""
import sys, io

XML = 'modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml'

with io.open(XML, encoding='utf-8') as f:
    src = f.read()

# (описание, old, new, ожидаемое кол-во)
EDITS = []

def edit(desc, old, new, count=1):
    EDITS.append((desc, old, new, count))

# ---------- 1. Toolbar (P1-3): заголовок + описание слева, expand на блок ----------
edit('toolbar expand + вставка title box',
'''                      expand="moreActionsPopUpButton"
                      align="MIDDLE_LEFT"
                      stylename="job-candidate-top-bar edit-toolbar">
                    <!-- Меню «Еще»: блокировка, подписка, аудит-справка. -->
                    <popupButton id="moreActionsPopUpButton"''',
'''                      expand="jobCandidateToolbarTitleBox"
                      align="MIDDLE_LEFT"
                      stylename="job-candidate-top-bar edit-toolbar">
                    <!-- Заголовок и описание формы слева (контракт edit-toolbar-title/-description). -->
                    <vbox id="jobCandidateToolbarTitleBox" width="100%" spacing="false"
                          stylename="job-candidate-toolbar-title-box">
                        <label value="msg://editorCaption" stylename="edit-toolbar-title" width="100%"/>
                        <label value="mainMsg://msgCandidate" stylename="edit-toolbar-description" width="100%"/>
                    </vbox>
                    <!-- Меню «Еще»: блокировка, подписка, аудит-справка. -->
                    <popupButton id="moreActionsPopUpButton"''', 1)

# ---------- 2. Секция «Социальные сети» (P2-9): height AUTO ----------
edit('секция соцсетей height AUTO',
     '                                  height="560px"\n                                  spacing="true"\n                                  expand="contactSocialNetworksContent"',
     '                                  height="AUTO"\n                                  spacing="true"\n                                  expand="contactSocialNetworksContent"', 1)

# ---------- 3. Мёртвые stylename (P2-10) ----------
edit('grid sidebar: мёртвые info/sidebar-grid',
     '<grid id="profileInfoGrid" spacing="true" width="100%"\n                          stylename="job-candidate-info-grid job-candidate-sidebar-grid">',
     '<grid id="profileInfoGrid" spacing="true" width="100%">', 1)

edit('hbox name-row (3 шт.)',
     'stylename="job-candidate-name-row">', '>', 3)

edit('карточки контактов: half-card/contact-card (2 шт.)',
     'stylename="job-candidate-card job-candidate-half-card job-candidate-contact-card edit-card"',
     'stylename="job-candidate-card edit-card"', 2)

edit('hbox positions-layout',
     '                                      spacing="true"\n                                      stylename="job-candidate-positions-layout">',
     '                                      spacing="true">', 1)

edit('dataGrid комментариев: table-comments',
     'stylename="no-horizontal-lines no-stripes no-vertical-lines borderless job-candidate-table job-candidate-table-comments"',
     'stylename="no-horizontal-lines no-stripes no-vertical-lines borderless job-candidate-table"', 1)

# ---------- 4. Captions колонок (P2-12) ----------
edit('колонка networkName: caption',
     '<column id="networkName" editable="false"\n                                                        property="socialNetworkURL.socialNetwork" maximumWidth="200px"/>',
     '<column id="networkName" editable="false"\n                                                        caption="msg://msgNetworkName"\n                                                        property="socialNetworkURL.socialNetwork" maximumWidth="200px"/>', 1)

edit('колонка vacancy (взаимодействия): caption',
     '<column id="vacancy" maximumWidth="400px" property="vacancy"/>',
     '<column id="vacancy" maximumWidth="400px" property="vacancy" caption="msg://msgVacancyName"/>', 1)

edit('колонка iteractionType: caption',
     '<column id="iteractionType" maximumWidth="250px" property="iteractionType"/>',
     '<column id="iteractionType" maximumWidth="250px" property="iteractionType" caption="msg://msgIteractionType"/>', 1)

edit('колонка recrutier: caption',
     '<column id="recrutier" maximumWidth="150px" property="recrutier"/>',
     '<column id="recrutier" maximumWidth="150px" property="recrutier" caption="msg://msgRecrutier"/>', 1)

# ---------- 5. Мелочи (P3-14) ----------
edit('birdhDateField: убрать width=AUTO',
     '                                                               property="birdhDate"\n                                                               stylename="edit-form-control"\n                                                               width="AUTO"/>',
     '                                                               property="birdhDate"\n                                                               stylename="edit-form-control"/>', 1)

edit('chatMessageTextField: убрать large',
     '<textField id="chatMessageTextField" stylename="large edit-form-control" width="100%"/>',
     '<textField id="chatMessageTextField" stylename="edit-form-control" width="100%"/>', 1)

edit('vacancyPopupPickerField: убрать large',
     '                                            <lookupPickerField id="vacancyPopupPickerField"\n                                                               align="MIDDLE_LEFT"\n                                                               stylename="large edit-form-control"',
     '                                            <lookupPickerField id="vacancyPopupPickerField"\n                                                               align="MIDDLE_LEFT"\n                                                               stylename="edit-form-control"', 1)

edit('groupBox well → job-candidate-card edit-card',
     '<groupBox stylename="well" spacing="true" width="100%">',
     '<groupBox stylename="job-candidate-card edit-card" spacing="true" width="100%">', 1)

# ---------- 6. Строки контактов: убрать width=100px + перевод (P2-11, P3-14) ----------
edit('label Email (контакты)',
     '<label value="Email" stylename="small" width="100px"/>',
     '<label value="msg://msgEmail" stylename="small"/>', 1)

edit('label Телефон (контакты)',
     '<label value="Телефон" stylename="small" width="100px"/>',
     '<label value="msg://msgPhone" stylename="small"/>', 1)

edit('label Мобильный (контакты)',
     '<label value="Мобильный" stylename="small" width="100px"/>',
     '<label value="msg://msgMobile" stylename="small"/>', 1)

edit('label Telegram (контакты)',
     '<label value="Telegram" stylename="small" width="100px"/>',
     '<label value="msg://msgTelegramAccount" stylename="small"/>', 1)

edit('label WhatsApp (контакты)',
     '<label value="WhatsApp" stylename="small" width="100px"/>',
     '<label value="msg://msgWhatsAppAccount" stylename="small"/>', 1)

edit('label Viber (контакты)',
     '<label value="Viber" stylename="small" width="100px"/>',
     '<label value="msg://msgViberAccount" stylename="small"/>', 1)

edit('label Skype (контакты)',
     '<label value="Skype" stylename="small" width="100px"/>',
     '<label value="msg://msgSkypeAccaunt" stylename="small"/>', 1)

# ---------- 7. Перевод прямых русских подписей (P2-11) ----------
# caption= (вкладки и nav-кнопки: одинаковые ключи, replace_all по числу вхождений)
edit('caption Основное (tab+nav, 2 шт.)', 'caption="Основное"', 'caption="msg://msgTabMain"', 2)
edit('caption Контакты (tab+nav, 2 шт.)', 'caption="Контакты"', 'caption="msg://msgCandidateContacts"', 2)
edit('caption Позиции и вакансии (tab+nav, 2 шт.)', 'caption="Позиции и вакансии"', 'caption="msg://msgTabPositions"', 2)
edit('caption Взаимодействия (tab+nav, 2 шт.)', 'caption="Взаимодействия"', 'caption="msg://msgCandidateIteraction"', 2)
edit('caption Резюме и файлы (tab+nav, 2 шт.)', 'caption="Резюме и файлы"', 'caption="msg://msgTabResume"', 2)
edit('caption Комментарии (tab+nav, 2 шт.)', 'caption="Комментарии"', 'caption="msg://msgComments"', 2)
edit('caption История (tab+nav, 2 шт.)', 'caption="История"', 'caption="msg://msgTabHistory"', 2)

# value= (заголовки секций и подписи)
edit('value Основное (заголовок секции)', 'value="Основное"', 'value="msg://msgTabMain"', 1)
edit('value Контакты (заголовок секции)', 'value="Контакты"', 'value="msg://msgCandidateContacts"', 1)
edit('value Позиции и вакансии (заголовок)', 'value="Позиции и вакансии"', 'value="msg://msgTabPositions"', 1)
edit('value Взаимодействия (заголовок)', 'value="Взаимодействия"', 'value="msg://msgCandidateIteraction"', 1)
edit('value Резюме и файлы (заголовок)', 'value="Резюме и файлы"', 'value="msg://msgTabResume"', 1)
edit('value Комментарии (заголовок)', 'value="Комментарии"', 'value="msg://msgComments"', 1)
edit('value История (заголовок)', 'value="История"', 'value="msg://msgTabHistory"', 1)

# Sidebar
edit('sidebar: Рейтинг', 'value="Рейтинг"', 'value="msg://msgRating"', 1)
edit('sidebar: Карточка', 'value="Карточка"', 'value="msg://msgCard"', 1)
edit('sidebar: Город (2 шт.)', 'value="Город"', 'value="mainMsg://msgCity"', 2)
edit('sidebar+форма: Компания (2 шт.)', 'value="Компания"', 'value="msg://msgCompany"', 2)
edit('sidebar: Резюме', 'value="Резюме"', 'value="msg://msgResume"', 1)
edit('sidebar: Разделы формы', 'value="Разделы формы"', 'value="msg://msgFormSections"', 1)
edit('sidebar: Email', 'value="Email"', 'value="msg://msgEmail"', 1)
edit('sidebar: Телефон', 'value="Телефон"', 'value="msg://msgPhone"', 1)
edit('sidebar: Telegram', 'value="Telegram"', 'value="msg://msgTelegramAccount"', 1)
edit('sidebar: Создать резюме', 'caption="Создать резюме"', 'caption="msg://msgCreateCV"', 1)
edit('sidebar: Создать взаимодействие', 'caption="Создать взаимодействие"', 'caption="msg://msgCreateIteraction"', 1)
edit('sidebar: HR-Мастер', 'caption="HR-Мастер"', 'caption="msg://msgOpenPositionMasterBrowse"', 1)
edit('toolbar: Еще', 'caption="Еще"', 'caption="msg://msgMore"', 1)
edit('popup: Создано/Изменено', 'caption="Создано/Изменено"', 'caption="msg://msgCreatedUpdated"', 1)

# Основное
edit('Основное: Персональные данные', 'value="Персональные данные"', 'value="msg://msgPersonalData"', 1)
edit('Основное: Имя', 'value="Имя"', 'value="msg://msgFirstName"', 1)
edit('Основное: Отчество', 'value="Отчество"', 'value="msg://msgMiddleName"', 1)
edit('Основное: Фамилия', 'value="Фамилия"', 'value="msg://msgSecondName"', 1)
edit('Основное: Дата рождения', 'value="Дата рождения"', 'value="msg://msgBirthDate"', 1)
edit('Основное: Должность', 'value="Должность"', 'value="msg://msgPersonPosition"', 1)
edit('Основное: Доп. позиции', 'value="Доп. позиции"', 'value="msg://msgAdditionalPositions"', 1)
edit('Основное: Профессиональные данные', 'value="Профессиональные данные"', 'value="msg://msgProfessionalData"', 1)

# Контакты
edit('Контакты: Основные контакты', 'value="Основные контакты"', 'value="msg://msgMainContacts"', 1)
edit('Контакты: Дополнительные контакты', 'value="Дополнительные контакты"', 'value="msg://msgAdditionalContacts"', 1)
edit('Контакты: Способ связи', 'value="Способ связи"', 'value="msg://msgPriorityCommunicationMethod"', 1)
edit('Контакты: Социальные сети', 'value="Социальные сети"', 'value="msg://msgSocialNetworks"', 1)

# История
edit('История: Данные записи', 'caption="Данные записи"', 'caption="msg://msgRecordData"', 1)
edit('История: Создал', 'value="Создал"', 'value="msg://msgCreatedByUser"', 1)
edit('История: Дата создания', 'value="Дата создания"', 'value="msg://msgCreateTsLabel"', 1)

# Footer
edit('footer: Сохранить и закрыть', 'caption="Сохранить и закрыть"', 'caption="msg://msgSaveAndClose"', 1)
edit('footer: Отмена', 'caption="Отмена"', 'caption="msg://msgCancel"', 1)

# ---------- применение ----------
ok = True
for desc, old, new, cnt in EDITS:
    n = src.count(old)
    if n != cnt:
        print('FAIL [%s]: ожидалось %d вхождений, найдено %d' % (desc, cnt, n))
        ok = False
    else:
        src = src.replace(old, new)

if not ok:
    sys.exit('Правки XML не применены — есть несовпадения.')

with io.open(XML, 'w', encoding='utf-8') as f:
    f.write(src)

print('XML применено правок: %d (все точные совпадения)' % len(EDITS))
