window.com_company_hunttech_web_extension_LlmChatLauncherExtension = function () {
    var connector = this;

    connector.initialize = function (storageKey, serverPosition) {
        var margin = 24;
        var threshold = 6;

        function attach(attempt) {
            var button = connector.getElement();
            var windowElement = (button && button.closest('.llm-chat-launcher-window'))
                || (button && button.closest('#llmChatLauncherWindow'))
                || document.getElementById('llmChatLauncherWindow')
                || document.querySelector('.llm-chat-launcher-window');

            if (!button || !windowElement) {
                if (attempt < 60) {
                    window.setTimeout(function () { attach(attempt + 1); }, 50);
                }
                return;
            }

            var dragging = false;
            var moved = false;
            var suppressClick = false;
            var hasCustomPosition = false;
            var startX = 0;
            var startY = 0;
            var startLeft = 0;
            var startTop = 0;

            button.setAttribute('aria-label', 'Открыть AI-чат');
            button.setAttribute('title', 'Открыть AI-чат');
            button.style.touchAction = 'none';
            windowElement.style.touchAction = 'none';

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
                var rect = windowElement.getBoundingClientRect();
                var width = rect.width || 56;
                var height = rect.height || 56;
                var maxLeft = Math.max(margin, window.innerWidth - width - margin);
                var maxTop = Math.max(margin, window.innerHeight - height - margin);
                var clampedLeft = Math.min(Math.max(margin, Math.round(left)), maxLeft);
                var clampedTop = Math.min(Math.max(margin, Math.round(top)), maxTop);

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
                if (saved && isFinite(saved.left) && isFinite(saved.top) && saved.left >= 0 && saved.top >= 0) {
                    applyPosition(saved.left, saved.top, true);
                    return;
                }
                // По умолчанию — правый нижний край экрана
                applyDefaultBottomRight();
            }

            function savePosition(left, top) {
                var roundLeft = Math.round(left);
                var roundTop = Math.round(top);
                if (!isFinite(roundLeft) || !isFinite(roundTop)) {
                    return;
                }
                var posJson = JSON.stringify({ left: roundLeft, top: roundTop });
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

                document.body.classList.remove('llm-chat-launcher-dragging');
                windowElement.classList.remove('llm-chat-launcher-dragging');

                if (moved) {
                    suppressClick = true;
                    var rect = windowElement.getBoundingClientRect();
                    savePosition(rect.left, rect.top);
                    if (event && event.cancelable) {
                        event.preventDefault();
                    }
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

            function attachDragListeners() {
                if (window.PointerEvent) {
                    window.addEventListener('pointermove', onPointerMove, { capture: true, passive: false });
                    window.addEventListener('pointerup', onPointerUp, { capture: true, passive: false });
                    window.addEventListener('pointercancel', onPointerUp, { capture: true, passive: false });
                } else {
                    window.addEventListener('mousemove', onMouseMove, { capture: true });
                    window.addEventListener('mouseup', onMouseUp, { capture: true });
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

                document.body.classList.add('llm-chat-launcher-dragging');
                windowElement.classList.add('llm-chat-launcher-dragging');
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
            }

            bindDragStart(button);
            bindDragStart(windowElement);

            button.addEventListener('click', function (event) {
                if (suppressClick) {
                    suppressClick = false;
                    event.preventDefault();
                    event.stopPropagation();
                    event.stopImmediatePropagation();
                    return false;
                }
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

        attach(0);
    };
};
