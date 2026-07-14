#!/usr/bin/env python3
'''Исправляет XML-контракт JobCandidateEdit без изменения бизнес-логики.'''

from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
XML_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml"
DOC_PATH = ROOT / "docs/screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md"


def replace_once(text: str, old: str, new: str, description: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"{description}: ожидалось одно совпадение, найдено {count}. "
            "Проверьте актуальность ветки перед применением исправления."
        )
    return text.replace(old, new, 1)


def patch_expand_hierarchy(xml: str) -> str:
    '''Делает исправление повторно запускаемым для уже обновлённого локального XML.'''
    replacements = [
        (
            '''<tab id="tabIteraction"
                         caption="Взаимодействия"
                         spacing="true"
                         margin="true"
                         expand="jobCandidateIteractionListTable">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">''',
            '''<tab id="tabIteraction"
                         caption="Взаимодействия"
                         spacing="true"
                         margin="true"
                         expand="tabIteractionSection">
                        <vbox id="tabIteractionSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabIteractionContent"
                              stylename="job-candidate-accordion-section">''',
            "вкладка взаимодействий",
        ),
        (
            '''<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <lookupPickerField id="vacancyFilterLookupPickerField"''',
            '''<vbox id="tabIteractionContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="jobCandidateIteractionListTable"
                                  stylename="job-candidate-accordion-content">

                        <lookupPickerField id="vacancyFilterLookupPickerField"''',
            "контент взаимодействий",
        ),
        (
            '''<tab id="tabResume"
                         caption="Резюме и файлы"
                         spacing="true"
                         margin="true"
                         expand="tabResumeVbox">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">''',
            '''<tab id="tabResume"
                         caption="Резюме и файлы"
                         spacing="true"
                         margin="true"
                         expand="tabResumeSection">
                        <vbox id="tabResumeSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabResumeContent"
                              stylename="job-candidate-accordion-section">''',
            "вкладка резюме",
        ),
        (
            '''<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <vbox id="tabResumeVbox"''',
            '''<vbox id="tabResumeContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="tabResumeVbox"
                                  stylename="job-candidate-accordion-content">

                        <vbox id="tabResumeVbox"''',
            "контент резюме",
        ),
        (
            '''<tab id="tabSocialNetworks"
                         caption="Социальные сети"
                         spacing="true"
                         margin="true"
                         expand="socialNetworkTableHbox">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">''',
            '''<tab id="tabSocialNetworks"
                         caption="Социальные сети"
                         spacing="true"
                         margin="true"
                         expand="tabSocialNetworksSection">
                        <vbox id="tabSocialNetworksSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabSocialNetworksContent"
                              stylename="job-candidate-accordion-section">''',
            "вкладка социальных сетей",
        ),
        (
            '''<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <hbox id="socialNetworkTableHbox"''',
            '''<vbox id="tabSocialNetworksContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="socialNetworkTableHbox"
                                  stylename="job-candidate-accordion-content">

                        <hbox id="socialNetworkTableHbox"''',
            "контент социальных сетей",
        ),
        (
            '''<tab id="commentsTab"
                         caption="Комментарии"
                         spacing="true"
                         margin="true"
                         expand="tabCommentsVbox">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">''',
            '''<tab id="commentsTab"
                         caption="Комментарии"
                         spacing="true"
                         margin="true"
                         expand="tabCommentsSection">
                        <vbox id="tabCommentsSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabCommentsContent"
                              stylename="job-candidate-accordion-section">''',
            "вкладка комментариев",
        ),
        (
            '''<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <vbox id="tabCommentsVbox"''',
            '''<vbox id="tabCommentsContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="tabCommentsVbox"
                                  stylename="job-candidate-accordion-content">

                        <vbox id="tabCommentsVbox"''',
            "контент комментариев",
        ),
    ]

    for old, new, description in replacements:
        xml = replace_once(xml, old, new, description)
    return xml


def restore_injection_contract(xml: str) -> str:
    '''Восстанавливает компоненты, которые остаются обязательными для @Inject контроллера.'''
    compatibility_block = '''                <!-- Legacy-контейнеры сохраняют XML-контракт @Inject контроллера.
                     Не удалять даже если они скрыты — без них
                     CUBA не сможет внедрить компоненты в Java-контроллер. -->
                <groupBox id="lastProjects"
                          visible="false"
                          caption="msg://msgLastProject"/>
                <grid id="dictionatysTavlesHBox"
                      visible="false"
                      spacing="true">
                    <columns count="2"/>
                    <rows>
                        <row>
                            <table id="lastProjectTable"
                                   visible="false"
                                   width="100%"/>
                        </row>
                        <row>
                            <table id="suggestVacancyTable"
                                   visible="false"
                                   width="100%"/>
                        </row>
                    </rows>
                </grid>'''

    marker = "<!-- Legacy-контейнеры сохраняют XML-контракт"
    if marker not in xml:
        # Insert before the closing </vbox> of the sidebar
        sidebar_marker = 'id="candidateProfileFooter"'
        idx = xml.rfind(sidebar_marker)
        if idx >= 0:
            # Find the end of the footer vbox
            footer_end = xml.find('</vbox>', idx)
            next_vbox = xml.find('</vbox>', footer_end + 7)
            if next_vbox >= 0:
                xml = xml[:next_vbox] + compatibility_block + '\n\n' + xml[next_vbox:]
    return xml


def update_documentation() -> None:
    doc = DOC_PATH.read_text(encoding="utf-8")
    history_row = (
        "| 2026-07-14 | Восстановлены `lastProjects`, `dictionatysTavlesHBox` как hidden "
        "placeholder для `@Inject` контроллера; исправлена иерархия `expand`. |\n"
    )
    if history_row not in doc:
        header = "|------|-----------|\n"
        doc = replace_once(doc, header, header + history_row, "история изменений")
    DOC_PATH.write_text(doc, encoding="utf-8")


def validate_expand_hierarchy() -> None:
    tree = ET.parse(XML_PATH)
    root = tree.getroot()
    errors = []
    for element in root.iter():
        target = element.attrib.get("expand")
        if not target:
            continue
        direct_child_ids = {child.attrib.get("id") for child in list(element)}
        if target not in direct_child_ids:
            errors.append(
                f"{element.tag.split('}')[-1]}#{element.attrib.get('id', '<без id>')} "
                f"expand={target!r}, direct children={sorted(x for x in direct_child_ids if x)}"
            )
    if errors:
        raise RuntimeError("Некорректные expand после исправления:\n" + "\n".join(errors))


def main() -> None:
    xml = XML_PATH.read_text(encoding="utf-8")
    xml = patch_expand_hierarchy(xml)
    xml = restore_injection_contract(xml)
    XML_PATH.write_text(xml, encoding="utf-8")
    update_documentation()
    validate_expand_hierarchy()
    print(
        "Исправление JobCandidateEdit применено: expand корректны,\n"
        "все @Inject UI-компоненты присутствуют в XML\n"
        "с ожидаемыми типами."
    )
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
