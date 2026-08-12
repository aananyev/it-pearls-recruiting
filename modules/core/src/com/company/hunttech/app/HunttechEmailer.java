package com.company.hunttech.app;

import com.haulmont.cuba.core.app.Emailer;
import com.haulmont.cuba.core.entity.SendingMessage;
import com.haulmont.cuba.core.global.SendingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Наследник платформенного Emailer с защитой от «писем-сирот».
 *
 * <p>Проблема платформы CUBA 7.3: если письмо сохранено в очередь с контентом в
 * FileStorage ({@code contentTextFile}), а сам файл потерян/удалён, то
 * {@code loadBodyAndAttachments()} молча логирует {@code FileStorageException} и
 * оставляет {@code contentText = null}. Затем {@code sendSendingMessage()} падает с
 * {@code NullPointerException} на {@code requireNonNull(contentText)} <b>вне</b>
 * try/catch — письмо навсегда остаётся в статусе SENDING, {@code attemptsMade}
 * не растёт, и планировщик пытается отправить его каждые {@code sendingTimeoutSec},
 * засоряя лог ошибками.</p>
 *
 * <p>Исправление: такие письма помечаются {@link SendingStatus#NOTSENT} с warn-логом
 * вместо вечного цикла. Регистрируется в spring.xml проекта под тем же именем
 * ({@code cuba_Emailer}), перекрывая платформенный бин.</p>
 *
 * <p>ВАЖНО: класс не аннотирован {@code @Component} — регистрация только в spring.xml
 * проекта (бин {@code cuba_Emailer}), чтобы гарантированно перекрыть платформенный
 * и не создавать дубликат определения.</p>
 */
public class HunttechEmailer extends Emailer {

    private static final Logger log = LoggerFactory.getLogger(HunttechEmailer.class);

    /**
     * Переопределяет отправку: если после загрузки тело/тема/адресат письма отсутствуют
     * (битое письмо), помечает NOTSENT вместо NPE вне try/catch. Это предотвращает
     * вечный цикл отправки писем с потерянным файлом контента.
     */
    @Override
    protected void sendSendingMessage(SendingMessage sendingMessage) {
        if (sendingMessage.getAddress() == null
                || sendingMessage.getCaption() == null
                || sendingMessage.getContentText() == null
                || sendingMessage.getFrom() == null) {
            log.warn("Письмо {} (to={}) повреждено: address={}, caption={}, contentText={}, from={}; помечаю NOTSENT",
                    sendingMessage.getId(), sendingMessage.getAddress(),
                    sendingMessage.getAddress() != null,
                    sendingMessage.getCaption() != null,
                    sendingMessage.getContentText() != null,
                    sendingMessage.getFrom() != null);
            markAsNonSent(sendingMessage);
            return;
        }
        super.sendSendingMessage(sendingMessage);
    }
}