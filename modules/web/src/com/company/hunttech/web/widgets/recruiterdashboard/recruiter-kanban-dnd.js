window.com_company_hunttech_web_widgets_recruiterdashboard_RecruiterKanbanDragDropExtension = function () {
    const connector = this;

    connector.initDragAndDrop = function () {
        // Вызывается с сервера при reinit()
    };

    let isDragging = false;
    let draggingCard = null;
    let ghost = null;
    let startRect = null;
    let startOffsetX = 0;
    let startOffsetY = 0;
    let startClientX = 0;
    let startClientY = 0;
    let cardId = null;
    let sourceStage = null;
    let currentHoveredColumn = null;
    let blockNextContextMenu = false;

    function clearHover() {
        if (currentHoveredColumn) {
            currentHoveredColumn.classList.remove('recruiter-kanban-column-drop-hover');
            currentHoveredColumn = null;
        }
        const allHovered = document.querySelectorAll('.recruiter-kanban-column-drop-hover');
        for (let i = 0; i < allHovered.length; i++) {
            allHovered[i].classList.remove('recruiter-kanban-column-drop-hover');
        }
    }

    function onMouseDown(e) {
        // Поддерживаем левую кнопку (0) и правую кнопку (2)
        if (e.button !== 0 && e.button !== 2) return;

        // Не начинаем перетаскивание при клике по интерактивным элементам (кнопки, ссылки, ввод)
        if (e.target.closest('button, .v-button, a, .v-linkbutton, input, textarea, select')) {
            return;
        }

        const card = e.target.closest('.recruiter-kanban-card');
        if (!card) return;

        const col = card.closest('.recruiter-kanban-column');
        if (!col) return;

        cardId = card.getAttribute('data-card-id') || (card.id ? card.id.replace(/^kanban-card-/, '') : null);
        sourceStage = col.getAttribute('data-stage') || (col.id ? col.id.replace(/^kanban-column-/, '') : null);
        if (!cardId || !sourceStage) return;

        // КРИТИЧНО: предотвращаем выделение текста браузером и запуск нативного Drag-and-Drop изображений
        e.preventDefault();
        if (e.button === 2) {
            blockNextContextMenu = true;
        }

        draggingCard = card;
        startRect = card.getBoundingClientRect();
        startOffsetX = e.clientX - startRect.left;
        startOffsetY = e.clientY - startRect.top;
        startClientX = e.clientX;
        startClientY = e.clientY;
        isDragging = false;

        document.addEventListener('mousemove', onMouseMove, true);
        document.addEventListener('mouseup', onMouseUp, true);
        window.addEventListener('blur', onMouseUp, true);
    }

    function onMouseMove(ev) {
        if (!draggingCard) return;

        if (!isDragging) {
            const dist = Math.hypot(ev.clientX - startClientX, ev.clientY - startClientY);
            if (dist < 4) return;

            isDragging = true;
            blockNextContextMenu = true;

            // Создаем ghost карточки
            ghost = draggingCard.cloneNode(true);
            ghost.className = draggingCard.className + ' recruiter-kanban-card-ghost';
            ghost.style.position = 'fixed';
            ghost.style.left = (ev.clientX - startOffsetX) + 'px';
            ghost.style.top = (ev.clientY - startOffsetY) + 'px';
            ghost.style.width = startRect.width + 'px';
            ghost.style.height = startRect.height + 'px';
            ghost.style.boxSizing = 'border-box';
            ghost.style.zIndex = '999999';
            ghost.style.pointerEvents = 'none'; // Гарантирует видимость колонки для elementFromPoint
            ghost.style.opacity = '0.92';
            ghost.style.transform = 'scale(1.03) rotate(1.5deg)';
            ghost.style.boxShadow = '0 16px 36px rgba(0, 0, 0, 0.32)';
            ghost.style.transition = 'none';
            ghost.style.cursor = 'grabbing';
            document.body.appendChild(ghost);

            draggingCard.style.opacity = '0.35';
            document.body.style.userSelect = 'none';
            document.body.style.cursor = 'grabbing';
        }

        if (ghost) {
            ghost.style.left = (ev.clientX - startOffsetX) + 'px';
            ghost.style.top = (ev.clientY - startOffsetY) + 'px';

            const elemBelow = document.elementFromPoint(ev.clientX, ev.clientY);
            const targetCol = elemBelow ? elemBelow.closest('.recruiter-kanban-column') : null;

            if (targetCol !== currentHoveredColumn) {
                clearHover();
                if (targetCol) {
                    const targetStageId = targetCol.getAttribute('data-stage') || (targetCol.id ? targetCol.id.replace(/^kanban-column-/, '') : null);
                    if (targetStageId !== sourceStage) {
                        currentHoveredColumn = targetCol;
                        currentHoveredColumn.classList.add('recruiter-kanban-column-drop-hover');
                    }
                }
            }
        }
    }

    function onMouseUp(ev) {
        document.removeEventListener('mousemove', onMouseMove, true);
        document.removeEventListener('mouseup', onMouseUp, true);
        window.removeEventListener('blur', onMouseUp, true);
        document.body.style.userSelect = '';
        document.body.style.cursor = '';

        if (!isDragging || !draggingCard) {
            draggingCard = null;
            if (ghost && ghost.parentNode) ghost.parentNode.removeChild(ghost);
            ghost = null;
            clearHover();
            return;
        }

        isDragging = false;
        const clientX = (ev && typeof ev.clientX === 'number') ? ev.clientX : startClientX;
        const clientY = (ev && typeof ev.clientY === 'number') ? ev.clientY : startClientY;
        const elemBelow = document.elementFromPoint(clientX, clientY);
        const targetCol = elemBelow ? elemBelow.closest('.recruiter-kanban-column') : null;
        let targetStage = null;
        if (targetCol) {
            targetStage = targetCol.getAttribute('data-stage');
            if (!targetStage && targetCol.id) {
                targetStage = targetCol.id.replace(/^kanban-column-/, '');
            }
        }

        clearHover();

        if (targetCol && targetStage && targetStage !== sourceStage) {
            if (ghost && ghost.parentNode) {
                ghost.parentNode.removeChild(ghost);
            }
            draggingCard.style.opacity = '1';
            connector.onCardMoved(cardId, targetStage);
        } else {
            // Возврат назад (fly-back)
            if (ghost && startRect) {
                ghost.style.transition = 'all 0.22s cubic-bezier(0.2, 0.8, 0.2, 1)';
                ghost.style.left = startRect.left + 'px';
                ghost.style.top = startRect.top + 'px';
                ghost.style.transform = 'none';
                ghost.style.opacity = '0.5';

                setTimeout(() => {
                    if (ghost && ghost.parentNode) {
                        ghost.parentNode.removeChild(ghost);
                    }
                    if (draggingCard) {
                        draggingCard.style.opacity = '1';
                    }
                }, 220);
            } else if (draggingCard) {
                draggingCard.style.opacity = '1';
            }
        }

        setTimeout(() => {
            blockNextContextMenu = false;
        }, 120);

        draggingCard = null;
        ghost = null;
    }

    function onContextMenu(e) {
        if (blockNextContextMenu || isDragging) {
            e.preventDefault();
            e.stopPropagation();
            blockNextContextMenu = false;
            return false;
        }
    }

    window.addEventListener('contextmenu', onContextMenu, true);
    document.addEventListener('mousedown', onMouseDown, true);

    connector.onUnregister = function () {
        document.removeEventListener('mousedown', onMouseDown, true);
        document.removeEventListener('mousemove', onMouseMove, true);
        document.removeEventListener('mouseup', onMouseUp, true);
        window.removeEventListener('blur', onMouseUp, true);
        window.removeEventListener('contextmenu', onContextMenu, true);
    };
};
