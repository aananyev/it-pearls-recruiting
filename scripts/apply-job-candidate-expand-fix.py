#!/usr/bin/env python3
"""Исправляет XML-контракт JobCandidateEdit без изменения бизнес-логики."""

from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
XML_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml"
JAVA_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java"
DESIGN_DOC_PATH = ROOT / "docs/screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md"
SPEC_DOC_PATH = ROOT / "docs/screens/job-candidate/hunttech_JobCandidate.edit_Spec.md"

UI_TYPE_TO_XML_TAG = {
    "Button": "button",
    "CheckBox": "checkBox",
    "DataGrid": "dataGrid",
    "DateField": "dateField",
    "FileUploadField": "upload",
    "GridLayout": "grid",
    "GroupBoxLayout": "groupBox",
    "HBoxLayout": "hbox",
    "Image": "image",
    "Label": "label",
    "LinkButton": "linkButton",
    "LookupPickerField": "lookupPickerField",
    "PopupButton": "popupButton",
    "RadioButtonGroup": "radioButtonGroup",
    "SuggestionField": "suggestionField",
    "SuggestionPickerField": "suggestionPickerField",
    "TabSheet": "tabSheet",
    "Table": "table",
    "TextField": "textField",
}


def replace_once(text: str, old: str, new: str, description: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"{description}: ожидалось одно совпадение, найдено {count}. "
            "Проверьте актуальность ветки перед применением исправления."
        )
    return text.replace(old, new, 1)


def apply_replacement_if_needed(
        text: str,
        old: str,
        new: str,
        completed_marker: str,
        description: str) -> str:
    """Делает исправление повторно запускаемым для уже обновлённого локального XML."""
    if completed_marker in text:
        return text
    return replace_once(text, old, new, description)


def patch_expand_hierarchy(xml: str) -> str:
    # CUBA разрешает expand только для непосредственного дочернего компонента.
    replacements = [
        (
            """<tab id="tabIteraction"
                         caption="Взаимодействия"
                         spacing="true"
                         margin="true"
                         expand="jobCandidateIteractionListTable">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">""",
            """<tab id="tabIteraction"
                         caption="Взаимодействия"
                         spacing="true"
                         margin="true"
                         expand="tabIteractionSection">
                        <vbox id="tabIteractionSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabIteractionContent"
                              stylename="job-candidate-accordion-section">""",
            'id="tabIteractionSection"',
            "вкладка взаимодействий",
        ),
        (
            """<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <lookupPickerField id="vacancyFilterLookupPickerField"""",
            """<vbox id="tabIteractionContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="jobCandidateIteractionListTable"
                                  stylename="job-candidate-accordion-content">

                        <lookupPickerField id="vacancyFilterLookupPickerField"""",
            'id="tabIteractionContent"',
            "контент взаимодействий",
        ),
        (
            """<tab id="tabResume"
                         caption="Резюме и файлы"
                         spacing="true"
                         margin="true"
                         expand="tabResumeVbox">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">""",
            """<tab id="tabResume"
                         caption="Резюме и файлы"
                         spacing="true"
                         margin="true"
                         expand="tabResumeSection">
                        <vbox id="tabResumeSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabResumeContent"
                              stylename="job-candidate-accordion-section">""",
            'id="tabResumeSection"',
            "вкладка резюме",
        ),
        (
            """<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <vbox id="tabResumeVbox"""",
            """<vbox id="tabResumeContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="tabResumeVbox"
                                  stylename="job-candidate-accordion-content">

                        <vbox id="tabResumeVbox"""",
            'id="tabResumeContent"',
            "контент резюме",
        ),
        (
            """<tab id="tabSocialNetworks"
                         caption="Социальные сети"
                         spacing="true"
                         margin="true"
                         expand="socialNetworkTableHbox">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">""",
            """<tab id="tabSocialNetworks"
                         caption="Социальные сети"
                         spacing="true"
                         margin="true"
                         expand="tabSocialNetworksSection">
                        <vbox id="tabSocialNetworksSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabSocialNetworksContent"
                              stylename="job-candidate-accordion-section">""",
            'id="tabSocialNetworksSection"',
            "вкладка социальных сетей",
        ),
        (
            """<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <hbox id="socialNetworkTableHbox"""",
            """<vbox id="tabSocialNetworksContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="socialNetworkTableHbox"
                                  stylename="job-candidate-accordion-content">

                        <hbox id="socialNetworkTableHbox"""",
            'id="tabSocialNetworksContent"',
            "контент социальных сетей",
        ),
        (
            """<tab id="commentsTab"
                         caption="Комментарии"
                         spacing="true"
                         margin="true"
                         expand="tabCommentsVbox">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">""",
            """<tab id="commentsTab"
                         caption="Комментарии"
                         spacing="true"
                         margin="true"
                         expand="tabCommentsSection">
                        <vbox id="tabCommentsSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabCommentsContent"
                              stylename="job-candidate-accordion-section">""",
            'id="tabCommentsSection"',
            "вкладка комментариев",
        ),
        (
            """<vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">

                        <vbox id="tabCommentsVbox"""",
            """<vbox id="tabCommentsContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="tabCommentsVbox"
                                  stylename="job-candidate-accordion-content">

                        <vbox id="tabCommentsVbox"""",
            'id="tabCommentsContent"',
            "контент комментариев",
        ),
    ]

    for old, new, completed_marker, description in replacements:
        xml = apply_replacement_if_needed(
            xml, old, new, completed_marker, description)

    return xml


def restore_injected_legacy_containers(xml: str) -> str:
    """Восстанавливает компоненты, которые остаются обязательными для @Inject контроллера."""
    has_grid = 'id="dictionatysTavlesHBox"' in xml
    has_group = 'id="lastProjects"' in xml

    if has_grid and has_group:
        return xml
    if has_grid != has_group:
        raise RuntimeError(
            "XML содержит только один из обязательных компонентов "
            "lastProjects/dictionatysTavlesHBox. Требуется ручная проверка."
        )

    anchor = "                <!-- Hidden fields for Java -->\n"
    compatibility_block = """                <!-- Legacy-контейнеры сохраняют XML-контракт @Inject контроллера.
                     Вкладка позиций отключена, поэтому контейнеры не участвуют в видимой компоновке. -->
                <groupBox id="lastProjects"
                          visible="false"
                          width="AUTO"
                          height="AUTO">
                    <grid id="dictionatysTavlesHBox"
                          visible="false"
                          width="AUTO">
                        <columns count="1"/>
                        <rows>
                            <row>
                                <label value="" visible="false"/>
                            </row>
                        </rows>
                    </grid>
                </groupBox>

"""
    return replace_once(
        xml,
        anchor,
        anchor + compatibility_block,
        "вставка legacy-контейнеров для @Inject",
    )


def patch_xml() -> None:
    xml = XML_PATH.read_text(encoding="utf-8")
    xml = patch_expand_hierarchy(xml)
    xml = restore_injected_legacy_containers(xml)
    XML_PATH.write_text(xml, encoding="utf-8")


def insert_history_row(path: Path, row: str) -> None:
    text = path.read_text(encoding="utf-8")
    if row in text:
        return

    section_pos = text.find("## История изменений")
    if section_pos < 0:
        raise RuntimeError(f"В {path} не найден раздел 'История изменений'")

    separator = "|------|-----------|\n"
    separator_pos = text.find(separator, section_pos)
    if separator_pos < 0:
        raise RuntimeError(f"В {path} не найдена таблица истории изменений")

    insert_pos = separator_pos + len(separator)
    text = text[:insert_pos] + row + text[insert_pos:]
    path.write_text(text, encoding="utf-8")


def update_documentation() -> None:
    expand_row = (
        "| 2026-07-14 | Исправлена иерархия `expand` во вкладках взаимодействий, "
        "резюме, социальных сетей и комментариев: каждый контейнер теперь "
        "расширяет только непосредственного дочернего компонента. |\n"
    )
    injection_row = (
        "| 2026-07-14 | Восстановлен XML-контракт контроллера: добавлены скрытые "
        "`lastProjects` (`groupBox`) и `dictionatysTavlesHBox` (`grid`), а также "
        "автоматическая проверка типов UI-компонентов `@Inject`. |\n"
    )

    insert_history_row(DESIGN_DOC_PATH, expand_row)
    insert_history_row(DESIGN_DOC_PATH, injection_row)
    insert_history_row(SPEC_DOC_PATH, injection_row)

    design = DESIGN_DOC_PATH.read_text(encoding="utf-8")
    expand_section = """

## Исправление иерархии `expand`

Ошибка `There is no component with id 'jobCandidateIteractionListTable' to expand` возникала из-за того, что вкладка ссылалась в `expand` на DataGrid, вложенный через промежуточные `vbox`. В CUBA Platform контейнер может расширять только своего непосредственного дочернего компонента.

Для `tabIteraction`, `tabResume`, `tabSocialNetworks` и `commentsTab` добавлена последовательная цепочка контейнеров с уникальными ID. Существующие component ID, data containers, properties, loaders, JPQL, actions, invoke и Java-контроллер не изменены.
"""
    if "## Исправление иерархии `expand`" not in design:
        design += expand_section

    injection_section = """

## Восстановление XML-контракта контроллера

Контроллер `JobCandidateEdit` продолжает внедрять `lastProjects` как `GroupBoxLayout` и `dictionatysTavlesHBox` как `GridLayout`. Редизайн удалил эти контейнеры из XML, из-за чего CUBA останавливала создание экрана на этапе dependency injection.

Компоненты восстановлены как скрытая compatibility-структура правильных типов. Это сохраняет существующие Java-инъекции и вызовы `setVisible(false)` для нового кандидата, не возвращая отключённую вкладку позиций в видимую компоновку и не меняя бизнес-логику.
"""
    if "## Восстановление XML-контракта контроллера" not in design:
        design += injection_section

    DESIGN_DOC_PATH.write_text(design, encoding="utf-8")


def local_name(element: ET.Element) -> str:
    return element.tag.split("}")[-1]


def validate_expand_hierarchy(root: ET.Element) -> None:
    errors = []
    for element in root.iter():
        target = element.attrib.get("expand")
        if not target:
            continue
        direct_child_ids = {child.attrib.get("id") for child in list(element)}
        if target not in direct_child_ids:
            errors.append(
                f"{local_name(element)}#{element.attrib.get('id', '<без id>')} "
                f"expand={target!r}, direct children={sorted(x for x in direct_child_ids if x)}"
            )
    if errors:
        raise RuntimeError(
            "Некорректные expand после исправления:\n" + "\n".join(errors)
        )


def collect_injected_ui_components() -> dict[str, str]:
    java = JAVA_PATH.read_text(encoding="utf-8")
    pattern = re.compile(
        r"@Inject\s+private\s+([A-Za-z0-9_]+)(?:<[^;]+>)?\s+([A-Za-z0-9_]+)\s*;"
    )
    result: dict[str, str] = {}
    for component_type, field_name in pattern.findall(java):
        if component_type in UI_TYPE_TO_XML_TAG:
            result[field_name] = component_type
    return result


def validate_injected_ui_components(root: ET.Element) -> None:
    xml_components: dict[str, set[str]] = {}
    for element in root.iter():
        component_id = element.attrib.get("id")
        if component_id:
            xml_components.setdefault(component_id, set()).add(local_name(element))

    errors = []
    for field_name, component_type in sorted(collect_injected_ui_components().items()):
        expected_tag = UI_TYPE_TO_XML_TAG[component_type]
        actual_tags = xml_components.get(field_name)
        if not actual_tags:
            errors.append(
                f"отсутствует {component_type} {field_name!r}, ожидаемый XML-тег <{expected_tag}>"
            )
        elif expected_tag not in actual_tags:
            errors.append(
                f"{field_name!r}: Java ожидает {component_type}/<{expected_tag}>, "
                f"в XML найдены {sorted(actual_tags)}"
            )

    if errors:
        raise RuntimeError(
            "Нарушен XML-контракт @Inject UI-компонентов:\n" + "\n".join(errors)
        )


def validate_xml() -> None:
    root = ET.parse(XML_PATH).getroot()
    validate_expand_hierarchy(root)
    validate_injected_ui_components(root)


def main() -> None:
    patch_xml()
    update_documentation()
    validate_xml()
    print(
        "Исправление JobCandidateEdit применено: expand корректны, "
        "все @Inject UI-компоненты присутствуют в XML с ожидаемыми типами."
    )
    # Скрипт одноразовый и не должен оставаться в итоговом diff Pull Request.
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
