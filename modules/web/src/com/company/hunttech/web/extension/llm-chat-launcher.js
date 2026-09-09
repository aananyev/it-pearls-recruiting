window.com_company_hunttech_web_extension_LlmChatLauncherExtension = function () {
    var connector = this;

    connector.initialize = function (storageKey, serverPosition) {
        const margin = 24;
        const threshold = 6;
        const MAX_COORDINATE_BOUND = 9000;
        let attached = false;
        let observer = null;
        let currentWindowElement = null;

        function attach(attempt) {
            if (attached && currentWindowElement && document.body.contains(currentWindowElement)) {
                return;
            }
            attached = false;

            // Попытка получить родительский элемент (llmChatLauncherWindow) и вложенную кнопку
            let windowElement = null;
            try {
                if (typeof connector.getElement === 'function') {
                    windowElement = connector.getElement()
                        || (typeof connector.getParentId === 'function' && connector.getElement(connector.getParentId()));
                }
            } catch (ignore) {
                windowElement = null;
            }

            if (!windowElement) {
                windowElement = document.getElementById('llmChatLauncherWindow')
                    || document.querySelector('.llm-chat-launcher-window');
            }

            let button = null;
            if (windowElement) {
                button = windowElement.querySelector('.llm-chat-launcher')
                    || windowElement.querySelector('.v-button')
                    || windowElement.querySelector('button');
            }
            if (!button) {
                button = document.getElementById('llmChatLauncher')
                    || document.querySelector('.llm-chat-launcher');
            }
            if (!windowElement && button) {
                windowElement = button.closest('.v-window')
                    || button.closest('.llm-chat-launcher-window');
            }

            if (!button || !windowElement) {
                if (attempt < 100) {
                    window.setTimeout(function () { attach(attempt + 1); }, 50);
                }
                return;
            }

            attached = true;
            currentWindowElement = windowElement;

            let dragging = false;
            let moved = false;
            let suppressClick = false;
            let hasCustomPosition = false;
            let startX = 0;
            let startY = 0;
            let startLeft = 0;
            let startTop = 0;
            let capturedTarget = null;
            let capturedPointerId = undefined;

            button.setAttribute('aria-label', 'Открыть AI-чат');
            button.setAttribute('title', 'Открыть AI-чат');
            button.style.touchAction = 'none';
            windowElement.style.touchAction = 'none';

            // Отключаем нативный HTML5 drag для SVG, путей и текстовых спанов
            const childElements = windowElement.querySelectorAll('svg, path, rect, span, .llm-chat-launcher-icon');
            for (let i = 0; i < childElements.length; i++) {
                childElements[i].style.pointerEvents = 'none';
                childElements[i].style.userSelect = 'none';
                childElements[i].setAttribute('draggable', 'false');
            }

            function applyDefaultBottomRight() {
                hasCustomPosition = false;
                windowElement.classList.remove('llm-chat-launcher-custom-position');
                windowElement.style.setProperty('left', 'auto', 'important');
                windowElement.style.setProperty('top', 'auto', 'important');
                windowElement.style.setProperty('right', margin + 'px', 'important');
                windowElement.style.setProperty('bottom', margin + 'px', 'important');
            }

            function applyPosition(left, top, isCustom) {
                if (!isCustom) {
                    applyDefaultBottomRight();
                    return;
                }
                const vpWidth = window.innerWidth || document.documentElement.clientWidth || 1024;
                const vpHeight = window.innerHeight || document.documentElement.clientHeight || 768;
                const rect = windowElement.getBoundingClientRect();
                const width = rect.width && rect.width > 0 ? rect.width : 56;
                const height = rect.height && rect.height > 0 ? rect.height : 56;
                const headerEl = document.querySelector('.c-app-menubar, .c-main-header, .v-menubar');
                let headerBottom = 0;
                if (headerEl && typeof headerEl.getBoundingClientRect === 'function') {
                    headerBottom = Math.round(headerEl.getBoundingClientRect().bottom);
                }
                const minTop = Math.max(headerBottom > 0 ? headerBottom + 8 : 64, margin);
                const maxLeft = Math.max(margin, vpWidth - width - margin);
                const maxTop = Math.max(minTop, vpHeight - height - margin);
                const clampedLeft = Math.min(Math.max(margin, Math.round(left)), maxLeft);
                const clampedTop = Math.min(Math.max(minTop, Math.round(top)), maxTop);

                hasCustomPosition = true;
                windowElement.classList.add('llm-chat-launcher-custom-position');
                windowElement.style.setProperty('left', clampedLeft + 'px', 'important');
                windowElement.style.setProperty('top', clampedTop + 'px', 'important');
                windowElement.style.setProperty('right', 'auto', 'important');
                windowElement.style.setProperty('bottom', 'auto', 'important');
            }

            function restorePosition() {
                var saved = null;
                if (serverPosition) {
                    try {
                        saved = typeof serverPosition === 'string' ? JSON.parse(serverPosition) : serverPosition;
                    } catch (ignore) {
                        saved = null;
                    }
                }
                if ((!saved || (saved.align !== 'bottom-right' && (!isFinite(saved.left) || !isFinite(saved.top)))) && storageKey) {
                    try {
                        saved = JSON.parse(window.localStorage.getItem(storageKey));
                    } catch (ignore) {
                        saved = null;
                    }
                }
                if (saved && saved.align === 'bottom-right') {
                    applyDefaultBottomRight();
                    return;
                }
                if (saved && isFinite(saved.left) && isFinite(saved.top) && saved.left >= 0 && saved.top >= 0 && saved.left < MAX_COORDINATE_BOUND && saved.top < MAX_COORDINATE_BOUND) {
                    applyPosition(saved.left, saved.top, true);
                    return;
                }
                // По умолчанию — правый нижний край экрана
                applyDefaultBottomRight();
            }

            function savePositionDefault() {
                const defJson = JSON.stringify({ align: 'bottom-right', right: margin, bottom: margin });
                serverPosition = defJson;
                if (storageKey) {
                    try {
                        window.localStorage.setItem(storageKey, defJson);
                    } catch (ignore) {}
                }
                if (typeof connector.savePosition === 'function') {
                    connector.savePosition(defJson);
                }
            }

            function savePosition(left, top) {
                var roundLeft = Math.round(left);
                var roundTop = Math.round(top);
                if (!isFinite(roundLeft) || !isFinite(roundTop)) {
                    return;
                }
                const posJson = JSON.stringify({ left: roundLeft, top: roundTop });
                serverPosition = posJson;
                if (storageKey) {
                    try {
                        window.localStorage.setItem(storageKey, posJson);
                    } catch (ignore) {
                        // Safe ignore
                    }
                }
                if (typeof connector.savePosition === 'function') {
                    connector.savePosition(posJson);
                }
            }

            function onMove(clientX, clientY, event) {
                if (!dragging) {
                    return;
                }
                var dx = clientX - startX;
                var dy = clientY - startY;
                if (!moved && Math.sqrt(dx * dx + dy * dy) < threshold) {
                    return;
                }
                moved = true;
                applyPosition(startLeft + dx, startTop + dy, true);
                if (event && event.cancelable) {
                    event.preventDefault();
                }
            }

            function onEnd(event) {
                if (!dragging) {
                    return;
                }
                dragging = false;
                detachDragListeners();

                if (capturedTarget && typeof capturedTarget.releasePointerCapture === 'function' && capturedPointerId !== undefined) {
                    try {
                        capturedTarget.releasePointerCapture(capturedPointerId);
                    } catch (ignore) {}
                    capturedTarget = null;
                    capturedPointerId = undefined;
                }

                document.body.classList.remove('llm-chat-launcher-dragging');
                windowElement.classList.remove('llm-chat-launcher-dragging');

                if (moved) {
                    suppressClick = true;
                    window.setTimeout(function () {
                        suppressClick = false;
                    }, 250);

                    var rect = windowElement.getBoundingClientRect();
                    savePosition(rect.left, rect.top);
                    if (event && event.cancelable) {
                        event.preventDefault();
                    }
                } else if (!hasCustomPosition) {
                    applyDefaultBottomRight();
                }
            }

            function onPointerMove(event) {
                onMove(event.clientX, event.clientY, event);
            }

            function onPointerUp(event) {
                onEnd(event);
            }

            function onMouseMove(event) {
                onMove(event.clientX, event.clientY, event);
            }

            function onMouseUp(event) {
                onEnd(event);
            }

            function onTouchMove(event) {
                if (event.touches && event.touches.length > 0) {
                    onMove(event.touches[0].clientX, event.touches[0].clientY, event);
                }
            }

            function onTouchEnd(event) {
                onEnd(event);
            }

            function attachDragListeners() {
                if (window.PointerEvent) {
                    window.addEventListener('pointermove', onPointerMove, { capture: true, passive: false });
                    window.addEventListener('pointerup', onPointerUp, { capture: true, passive: false });
                    window.addEventListener('pointercancel', onPointerUp, { capture: true, passive: false });
                } else {
                    window.addEventListener('mousemove', onMouseMove, { capture: true });
                    window.addEventListener('mouseup', onMouseUp, { capture: true });
                    window.addEventListener('touchmove', onTouchMove, { capture: true, passive: false });
                    window.addEventListener('touchend', onTouchEnd, { capture: true, passive: false });
                    window.addEventListener('touchcancel', onTouchEnd, { capture: true, passive: false });
                }
            }

            function detachDragListeners() {
                if (window.PointerEvent) {
                    window.removeEventListener('pointermove', onPointerMove, { capture: true, passive: false });
                    window.removeEventListener('pointerup', onPointerUp, { capture: true, passive: false });
                    window.removeEventListener('pointercancel', onPointerUp, { capture: true, passive: false });
                }
                window.removeEventListener('mousemove', onMouseMove, { capture: true });
                window.removeEventListener('mouseup', onMouseUp, { capture: true });
                window.removeEventListener('touchmove', onTouchMove, { capture: true, passive: false });
                window.removeEventListener('touchend', onTouchEnd, { capture: true, passive: false });
                window.removeEventListener('touchcancel', onTouchEnd, { capture: true, passive: false });
            }

            function onStart(clientX, clientY, event) {
                if (dragging || (event.button !== undefined && event.button !== 0)) {
                    return;
                }
                var rect = windowElement.getBoundingClientRect();
                dragging = true;
                moved = false;
                startX = clientX;
                startY = clientY;
                startLeft = rect.left;
                startTop = rect.top;

                if (event.pointerId !== undefined && event.target && typeof event.target.setPointerCapture === 'function') {
                    try {
                        event.target.setPointerCapture(event.pointerId);
                        capturedTarget = event.target;
                        capturedPointerId = event.pointerId;
                    } catch (ignore) {}
                }

                document.body.classList.add('llm-chat-launcher-dragging');
                windowElement.classList.add('llm-chat-launcher-dragging');
                windowElement.classList.add('llm-chat-launcher-custom-position');
                windowElement.style.setProperty('right', 'auto', 'important');
                windowElement.style.setProperty('bottom', 'auto', 'important');
                windowElement.style.setProperty('left', Math.round(startLeft) + 'px', 'important');
                windowElement.style.setProperty('top', Math.round(startTop) + 'px', 'important');

                attachDragListeners();
            }

            function bindDragStart(element) {
                if (!element) return;

                element.addEventListener('pointerdown', function (event) {
                    onStart(event.clientX, event.clientY, event);
                }, { capture: true });

                element.addEventListener('mousedown', function (event) {
                    if (!window.PointerEvent) {
                        onStart(event.clientX, event.clientY, event);
                    }
                }, { capture: true });

                element.addEventListener('touchstart', function (event) {
                    if (!window.PointerEvent && event.touches && event.touches.length > 0) {
                        onStart(event.touches[0].clientX, event.touches[0].clientY, event);
                    }
                }, { capture: true, passive: false });

                element.addEventListener('dragstart', function (event) {
                    event.preventDefault();
                    return false;
                }, true);
            }

            bindDragStart(windowElement);
            bindDragStart(button);

            button.addEventListener('click', function (event) {
                if (suppressClick) {
                    suppressClick = false;
                    event.preventDefault();
                    event.stopPropagation();
                    event.stopImmediatePropagation();
                    return false;
                }
            }, true);

            // Правый клик (contextmenu) сбрасывает лаунчер в безопасный правый нижний угол без открытия диалога чата
            button.addEventListener('contextmenu', function (event) {
                event.preventDefault();
                event.stopPropagation();
                applyDefaultBottomRight();
                savePositionDefault();
            }, true);

            window.addEventListener('resize', function () {
                if (dragging) {
                    return;
                }
                if (hasCustomPosition) {
                    var rect = windowElement.getBoundingClientRect();
                    applyPosition(rect.left, rect.top, true);
                } else {
                    applyDefaultBottomRight();
                }
            });

            // Восстанавливаем позицию сразу и контрольно через микротаймаут
            restorePosition();
            window.setTimeout(function () {
                if (!hasCustomPosition && !dragging) {
                    restorePosition();
                }
            }, 100);
        }

        if (window.MutationObserver) {
            observer = new MutationObserver(function () {
                if (!attached) {
                    attach(0);
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
        }

        attach(0);
    };
};
