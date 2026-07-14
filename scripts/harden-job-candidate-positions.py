#!/usr/bin/env python3
"""Усиливает восстановленную вкладку позиций без изменения её бизнес-правил."""

from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java"
XML_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml"


def replace_once(text: str, old: str, new: str, description: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{description}: ожидалось одно совпадение, найдено {count}")
    return text.replace(old, new, 1)


def patch_java() -> None:
    java = JAVA_PATH.read_text(encoding="utf-8")

    java = replace_once(
        java,
        '                                        "and vacancy is not null " +',
        '                                        "and vacancy.id is not null " +',
        "проверка vacancy в JPQL",
    )

    old_done = '''                        lastProjectDl.setParameter("candidate", getEditedEntity());
                        lastProjectDl.load();
                        setLastProjectOfCandidate();
                        setSuggestOpenPositionTable();
                        lastProjectTable.repaint();
                        suggestVacancyTable.repaint();'''

    new_done = '''                        try {
                            lastProjectDl.setParameter("candidate", getEditedEntity());
                            lastProjectDl.load();
                            setLastProjectOfCandidate();
                            setSuggestOpenPositionTable();
                            lastProjectTable.repaint();
                            suggestVacancyTable.repaint();
                        } catch (RuntimeException loaderException) {
                            // Разрешаем повторное открытие вкладки после временной ошибки БД.
                            positionsTabLoaded = false;
                            log.error("Не удалось применить данные вкладки позиций, candidateId={}",
                                    candidateId, loaderException);
                            notifications.create(Notifications.NotificationType.ERROR)
                                    .withCaption(messageBundle.getMessage("msgError"))
                                    .withDescription("Не удалось загрузить позиции и вакансии кандидата")
                                    .show();
                        }'''
    java = replace_once(java, old_done, new_done, "обработка UI-loader'ов")

    old_parameters = '''                suggestOpenPositionDl.setParameter("positionType", getEditedEntity().getPersonPosition());
                if (positions.size() > 0) {
                    suggestOpenPositionDl.setParameter("positionTypes", positions);
                }

            }'''

    new_parameters = '''                Position mainPosition = getEditedEntity().getPersonPosition();
                if (mainPosition != null) {
                    suggestOpenPositionDl.setParameter("positionType", mainPosition);
                } else {
                    suggestOpenPositionDl.removeParameter("positionType");
                }
                if (positions.size() > 0) {
                    suggestOpenPositionDl.setParameter("positionTypes", positions);
                } else {
                    suggestOpenPositionDl.removeParameter("positionTypes");
                }

            }'''
    java = replace_once(java, old_parameters, new_parameters, "параметры подходящих вакансий")

    pattern = re.compile(
        r'''    @Install\(to = "suggestVacancyTable", subject = "itemDescriptionProvider"\)\n'''
        r'''    private String suggestVacancyTableItemDescriptionProvider\(OpenPosition openPosition, String string\) \{.*?\n'''
        r'''    \}\n\n'''
        r'''    @Install\(to = "suggestVacancyTable.notSendedIconColumn", subject = "columnGenerator"\)''',
        re.DOTALL,
    )

    replacement = '''    @Install(to = "suggestVacancyTable", subject = "itemDescriptionProvider")
    private String suggestVacancyTableItemDescriptionProvider(OpenPosition openPosition, String string) {
        StringBuilder sb = new StringBuilder("<b>Вакансия:</b><br><br>");
        sb.append("<i>")
                .append(openPosition.getVacansyName() != null ? openPosition.getVacansyName() : "")
                .append("</i><br>");

        Project project = openPosition.getProjectName();
        if (project != null) {
            sb.append("<i>Проект: </i>")
                    .append(project.getProjectName() != null ? project.getProjectName() : "")
                    .append("<br>");

            Person projectOwner = project.getProjectOwner();
            if (projectOwner != null) {
                sb.append("<i>Ответственный за проект у заказчика: </i>")
                        .append(projectOwner.getSecondName() != null ? projectOwner.getSecondName() : "")
                        .append(" ")
                        .append(projectOwner.getFirstName() != null ? projectOwner.getFirstName() : "")
                        .append("<br>");
            }
        }

        if (openPosition.getOwner() != null) {
            sb.append("<i>Ответственный за проект на нашей стороне: </i>")
                    .append(openPosition.getOwner().getName() != null
                            ? openPosition.getOwner().getName() : "")
                    .append("<br>");
        }
        if (openPosition.getLastOpenDate() != null) {
            sb.append("<i>Дата открытия вакансии: </i>")
                    .append(openPosition.getLastOpenDate())
                    .append("<br>");
        }
        if (openPosition.getComment() != null) {
            sb.append("<br><i>Описание вакансии: </i><br>")
                    .append(openPosition.getComment());
        }

        return sb.toString();
    }

    @Install(to = "suggestVacancyTable.notSendedIconColumn", subject = "columnGenerator")'''

    java, count = pattern.subn(replacement, java, count=1)
    if count != 1:
        raise RuntimeError(f"null-safe tooltip: ожидалось одно совпадение, найдено {count}")

    JAVA_PATH.write_text(java, encoding="utf-8")


def patch_xml() -> None:
    xml = XML_PATH.read_text(encoding="utf-8")
    old_view = '''        <collection id="suggestOpenPositionDc"
                    class="com.company.hunttech.entity.OpenPosition">
            <view extends="_local"/>'''
    new_view = '''        <collection id="suggestOpenPositionDc"
                    class="com.company.hunttech.entity.OpenPosition">
            <!-- Поля ограничены данными таблицы и её tooltip; тяжёлые связи не загружаются. -->
            <view extends="_local">
                <property name="positionType" view="_local"/>
                <property name="projectName" view="_local">
                    <property name="projectOwner" view="_local"/>
                </property>
                <property name="owner" view="_local"/>
            </view>'''
    xml = replace_once(xml, old_view, new_view, "view подходящих вакансий")
    XML_PATH.write_text(xml, encoding="utf-8")
    ET.parse(XML_PATH)


def main() -> None:
    patch_java()
    patch_xml()
    print("Вкладка позиций усилена против unfetched/null loader ошибок.")


if __name__ == "__main__":
    main()
