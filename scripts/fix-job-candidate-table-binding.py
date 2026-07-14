#!/usr/bin/env python3
"""Исправляет дубли таблиц без data binding в JobCandidateEdit."""

from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
XML_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml"
DOC_PATH = ROOT / "docs/screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md"

BROKEN_BLOCK = '''                <!-- Legacy-контейнеры сохраняют XML-контракт @Inject контроллера.
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

FIXED_BLOCK = '''                <!-- Legacy-контейнеры сохраняют XML-контракт @Inject контроллера.
                     Таблицы здесь не дублируются: их рабочие экземпляры находятся
                     во вкладке «Позиции и вакансии» и имеют штатные dataContainer. -->
                <groupBox id="lastProjects"
                          visible="false"
                          width="1px"
                          height="1px"
                          caption="msg://msgLastProject"
                          expand="dictionatysTavlesHBox">
                    <grid id="dictionatysTavlesHBox"
                          visible="false"
                          width="1px"
                          height="1px"
                          spacing="false">
                        <columns count="1"/>
                        <rows>
                            <row>
                                <label value=""/>
                            </row>
                        </rows>
                    </grid>
                </groupBox>'''


def patch_xml() -> None:
    xml = XML_PATH.read_text(encoding="utf-8")

    if BROKEN_BLOCK in xml:
        xml = xml.replace(BROKEN_BLOCK, FIXED_BLOCK, 1)
    elif FIXED_BLOCK not in xml:
        raise RuntimeError(
            "Не найден ожидаемый compatibility-блок. "
            "Проверьте актуальность ветки и не применяйте исправление вручную."
        )

    XML_PATH.write_text(xml, encoding="utf-8")


def local_name(element: ET.Element) -> str:
    return element.tag.split("}")[-1]


def validate_xml() -> None:
    root = ET.parse(XML_PATH).getroot()
    elements = list(root.iter())

    # Контроллер ожидает ровно по одному компоненту каждого типа.
    expected = {
        ("groupBox", "lastProjects"): 1,
        ("grid", "dictionatysTavlesHBox"): 1,
        ("table", "lastProjectTable"): 1,
        ("table", "suggestVacancyTable"): 1,
    }
    for (tag, component_id), expected_count in expected.items():
        actual = sum(
            1 for element in elements
            if local_name(element) == tag and element.attrib.get("id") == component_id
        )
        if actual != expected_count:
            raise RuntimeError(
                f"Компонент {tag}#{component_id}: ожидалось {expected_count}, найдено {actual}"
            )

    # Все таблицы и DataGrid формы обязаны иметь data binding.
    unbound = []
    for element in elements:
        tag = local_name(element)
        if tag not in {"table", "dataGrid", "treeTable"}:
            continue
        if not (element.attrib.get("dataContainer") or element.attrib.get("datasource")):
            unbound.append(f"{tag}#{element.attrib.get('id', '<без id>')}")

    if unbound:
        raise RuntimeError(
            "Компоненты таблиц без data binding: " + ", ".join(unbound)
        )

    bindings = {
        "lastProjectTable": "lastProjectDc",
        "suggestVacancyTable": "suggestOpenPositionDc",
    }
    for component_id, expected_container in bindings.items():
        component = next(
            element for element in elements
            if local_name(element) == "table" and element.attrib.get("id") == component_id
        )
        actual_container = component.attrib.get("dataContainer")
        if actual_container != expected_container:
            raise RuntimeError(
                f"table#{component_id}: ожидался dataContainer={expected_container!r}, "
                f"получен {actual_container!r}"
            )

    # Проверка правила CUBA: expand указывает только на прямого потомка.
    expand_errors = []
    for element in elements:
        target = element.attrib.get("expand")
        if not target:
            continue
        child_ids = {child.attrib.get("id") for child in list(element)}
        if target not in child_ids:
            expand_errors.append(
                f"{local_name(element)}#{element.attrib.get('id', '<без id>')} -> {target}"
            )
    if expand_errors:
        raise RuntimeError(
            "Некорректные expand: " + ", ".join(expand_errors)
        )


def update_documentation() -> None:
    doc = DOC_PATH.read_text(encoding="utf-8")
    row = (
        "| 2026-07-14 | Удалены ошибочные дубли `lastProjectTable` и "
        "`suggestVacancyTable` без `dataContainer`; рабочие таблицы сохранены "
        "в единственном экземпляре со штатной привязкой данных. |\n"
    )
    if row not in doc:
        marker = "|------|-----------|\n"
        if marker not in doc:
            raise RuntimeError("В документации не найдена таблица истории изменений")
        doc = doc.replace(marker, marker + row, 1)

    section = '''

## Исправление data binding таблиц

Ошибка `Table doesn't have data binding` возникала из-за ошибочного восстановления в compatibility-блоке вторых экземпляров `lastProjectTable` и `suggestVacancyTable` без атрибута `dataContainer`. Рабочие таблицы уже существуют во вкладке «Позиции и вакансии» и связаны соответственно с `lastProjectDc` и `suggestOpenPositionDc`.

Из compatibility-блока удалены только дубли таблиц. Компоненты `lastProjects` (`groupBox`) и `dictionatysTavlesHBox` (`grid`), необходимые для `@Inject` в контроллере, сохранены. Java-контроллер, loaders, JPQL, actions, invoke и бизнес-логика не изменялись.
'''
    if "## Исправление data binding таблиц" not in doc:
        doc += section

    DOC_PATH.write_text(doc, encoding="utf-8")


def main() -> None:
    patch_xml()
    validate_xml()
    update_documentation()
    print(
        "Исправление применено: дубли таблиц удалены, "
        "все table/dataGrid имеют data binding."
    )
    # Одноразовый технический скрипт не должен оставаться в итоговом diff.
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
