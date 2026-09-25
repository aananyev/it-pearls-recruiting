window.com_company_hunttech_web_widgets_recruiterdashboard_RecruiterKanbanDragDropExtension = function () {
    const connector = this;

    connector.initDragAndDrop = function () {
        setupBoardListeners();
    };

    function setupBoardListeners() {
        const board = document.querySelector('.recruiter-kanban-board');
        if (!board) return;

        if (board._kanbanDndInitialized) return;
        board._kanbanDndInitialized = true;

        let isDragging = false;
        let draggingCard = null;
        let ghost = null;
        let startRect = null;
        let startOffsetX = 0;
        let startOffsetY = 0;
        let cardId = null;
        let sourceStage = null;
        let currentHoveredColumn = null;
        let blockNextContextMenu = false;

        let activeMouseMoveHandler = null;
        let activeMouseUpHandler = null;

        function clearHover() {
            if (currentHoveredColumn) {
                currentHoveredColumn.classList.remove('recruiter-kanban-column-drop-hover');
                currentHoveredColumn = null;
            }
        }

        function cleanupDragListeners() {
            if (activeMouseMoveHandler) {
                window.removeEventListener('mousemove', activeMouseMoveHandler, true);
                activeMouseMoveHandler = null;
            }
            if (activeMouseUpHandler) {
                window.removeEventListener('mouseup', activeMouseUpHandler, true);
                window.removeEventListener('blur', activeMouseUpHandler, true);
                activeMouseUpHandler = null;
            }
            clearHover();
        }

        window.addEventListener('contextmenu', (e) => {
            if (blockNextContextMenu || isDragging) {
                e.preventDefault();
                e.stopPropagation();
                blockNextContextMenu = false;
                return false;
            }
        }, true);

        board.addEventListener('mousedown', (e) => {
            // Поддержка правой клавиши мыши (button === 2) - приоритетное требование пользователя,
            // а также поддержка левой клавиши (button === 0)
            if (e.button !== 2 && e.button !== 0) return;

            // Если клик на ссылке, кнопке профиля или элементе ввода - не начинаем drag
            if (e.target.closest('button, .v-button, a, .v-linkbutton, input, textarea')) {
                return;
            }

            const card = e.target.closest('.recruiter-kanban-card');
            if (!card) return;

            const col = card.closest('.recruiter-kanban-column');
            if (!col) return;

            cardId = card.getAttribute('data-card-id') || (card.id ? card.id.replace(/^kanban-card-/, '') : null);
            sourceStage = col.getAttribute('data-stage') || (col.id ? col.id.replace(/^kanban-column-/, '') : null);
            if (!cardId || !sourceStage) return;

            if (e.button === 2) {
                blockNextContextMenu = true;
                e.preventDefault();
            }

            cleanupDragListeners();

            draggingCard = card;
            startRect = card.getBoundingClientRect();
            startOffsetX = e.clientX - startRect.left;
            startOffsetY = e.clientY - startRect.top;

            function onMouseMove(ev) {
                if (!isDragging) {
                    const dist = Math.hypot(ev.clientX - (startRect.left + startOffsetX), ev.clientY - (startRect.top + startOffsetY));
                    if (dist < 4) return;

                    isDragging = true;
                    blockNextContextMenu = true;

                    // Создаем плавающий ghost
                    ghost = draggingCard.cloneNode(true);
                    ghost.className = draggingCard.className + ' recruiter-kanban-card-ghost';
                    ghost.style.position = 'fixed';
                    ghost.style.left = startRect.left + 'px';
                    ghost.style.top = startRect.top + 'px';
                    ghost.style.width = startRect.width + 'px';
                    ghost.style.height = startRect.height + 'px';
                    ghost.style.boxSizing = 'border-box';
                    ghost.style.zIndex = '999999';
                    ghost.style.pointerEvents = 'none';
                    ghost.style.opacity = '0.92';
                    ghost.style.transform = 'scale(1.03) rotate(1.5deg)';
                    ghost.style.boxShadow = '0 16px 36px rgba(0, 0, 0, 0.28)';
                    ghost.style.transition = 'none';
                    ghost.style.cursor = 'grabbing';
                    document.body.appendChild(ghost);

                    draggingCard.style.opacity = '0.35';
                }

                if (ghost) {
                    ghost.style.left = (ev.clientX - startOffsetX) + 'px';
                    ghost.style.top = (ev.clientY - startOffsetY) + 'px';

                    const elemBelow = document.elementFromPoint(ev.clientX, ev.clientY);
                    const targetCol = elemBelow ? elemBelow.closest('.recruiter-kanban-column') : null;

                    if (targetCol !== currentHoveredColumn) {
                        clearHover();
                        if (targetCol && targetCol.getAttribute('data-stage') !== sourceStage) {
                            currentHoveredColumn = targetCol;
                            currentHoveredColumn.classList.add('recruiter-kanban-column-drop-hover');
                        }
                    }
                }
            }

            function onMouseUp(ev) {
                cleanupDragListeners();

                if (!isDragging) {
                    draggingCard = null;
                    ghost = null;
                    return;
                }

                isDragging = false;
                const clientX = (ev && typeof ev.clientX === 'number') ? ev.clientX : (startRect ? startRect.left : 0);
                const clientY = (ev && typeof ev.clientY === 'number') ? ev.clientY : (startRect ? startRect.top : 0);
                const elemBelow = document.elementFromPoint(clientX, clientY);
                const targetCol = elemBelow ? elemBelow.closest('.recruiter-kanban-column') : null;
                const targetStage = targetCol ? (targetCol.getAttribute('data-stage') || (targetCol.id ? targetCol.id.replace(/^kanban-column-/, '') : null)) : null;

                if (targetCol && targetStage && targetStage !== sourceStage) {
                    // Перемещение в другую колонку: серверный вызов диалога IteractionListEdit
                    if (ghost && ghost.parentNode) {
                        ghost.parentNode.removeChild(ghost);
                    }
                    if (draggingCard) {
                        draggingCard.style.opacity = '1';
                    }
                    connector.onCardMoved(cardId, targetStage);
                } else {
                    // Отменено или та же колонка: плавный возврат карточки на исходную позицию (fly-back)
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

            activeMouseMoveHandler = onMouseMove;
            activeMouseUpHandler = onMouseUp;
            window.addEventListener('mousemove', onMouseMove, true);
            window.addEventListener('mouseup', onMouseUp, true);
            window.addEventListener('blur', onMouseUp, true);
        });
    }

    // Инициализация при монтировании
    setTimeout(setupBoardListeners, 100);

    // Поддержка перестройки колонок через MutationObserver (с сохранением единственного экземпляра)
    if (window.__recruiterKanbanDndObserver) {
        window.__recruiterKanbanDndObserver.disconnect();
    }
    const observer = new MutationObserver(() => {
        const board = document.querySelector('.recruiter-kanban-board');
        if (board && !board._kanbanDndInitialized) {
            setupBoardListeners();
        }
    });
    observer.observe(document.body, { childList: true, subtree: true });
    window.__recruiterKanbanDndObserver = observer;

    connector.onUnregister = function () {
        if (window.__recruiterKanbanDndObserver) {
            window.__recruiterKanbanDndObserver.disconnect();
            window.__recruiterKanbanDndObserver = null;
        }
    };
};
