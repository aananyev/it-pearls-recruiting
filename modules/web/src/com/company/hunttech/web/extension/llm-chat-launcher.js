window.com_company_hunttech_web_extension_LlmChatLauncherExtension = function () {
    var connector = this;

    connector.initialize = function (storageKey) {
        function attach(attempt) {
            var button = connector.getElement();
            var windowElement = button && button.closest('.llm-chat-launcher-window');
            if (!windowElement) {
                if (attempt < 10) {
                    window.setTimeout(function () { attach(attempt + 1); }, 50);
                }
                return;
            }

            var threshold = 6;
            var margin = 24;
            var dragging = false;
            var moved = false;
            var suppressClick = false;
            var startX = 0;
            var startY = 0;
            var startLeft = 0;
            var startTop = 0;

            button.setAttribute('aria-label', 'Открыть AI-чат');
            button.setAttribute('title', 'Открыть AI-чат');
            button.style.touchAction = 'none';

            function clampPosition() {
                var rect = windowElement.getBoundingClientRect();
                var maxLeft = Math.max(margin, window.innerWidth - rect.width - margin);
                var maxTop = Math.max(margin, window.innerHeight - rect.height - margin);
                var left = parseFloat(windowElement.style.left);
                var top = parseFloat(windowElement.style.top);

                if (!isFinite(left)) {
                    left = maxLeft;
                }
                if (!isFinite(top)) {
                    top = maxTop;
                }
                windowElement.style.left = Math.min(Math.max(margin, left), maxLeft) + 'px';
                windowElement.style.top = Math.min(Math.max(margin, top), maxTop) + 'px';
                windowElement.style.right = 'auto';
                windowElement.style.bottom = 'auto';
            }

            function restorePosition() {
                var saved = null;
                try {
                    saved = JSON.parse(window.localStorage.getItem(storageKey));
                } catch (ignore) {
                    saved = null;
                }
                if (saved && isFinite(saved.left) && isFinite(saved.top)) {
                    windowElement.style.left = saved.left + 'px';
                    windowElement.style.top = saved.top + 'px';
                    clampPosition();
                    return;
                }
                windowElement.style.left = (window.innerWidth - windowElement.offsetWidth - margin) + 'px';
                windowElement.style.top = (window.innerHeight - windowElement.offsetHeight - margin) + 'px';
                clampPosition();
            }

            function savePosition() {
                try {
                    window.localStorage.setItem(storageKey, JSON.stringify({
                        left: parseFloat(windowElement.style.left),
                        top: parseFloat(windowElement.style.top)
                    }));
                } catch (ignore) {
                    // Private browsing or disabled storage must not disable the launcher.
                }
            }

            function onPointerDown(event) {
                if (event.button !== undefined && event.button !== 0) {
                    return;
                }
                var rect = windowElement.getBoundingClientRect();
                dragging = true;
                moved = false;
                startX = event.clientX;
                startY = event.clientY;
                startLeft = rect.left;
                startTop = rect.top;
                if (button.setPointerCapture && event.pointerId !== undefined) {
                    button.setPointerCapture(event.pointerId);
                }
            }

            function onPointerMove(event) {
                if (!dragging) {
                    return;
                }
                var dx = event.clientX - startX;
                var dy = event.clientY - startY;
                if (!moved && Math.sqrt(dx * dx + dy * dy) < threshold) {
                    return;
                }
                moved = true;
                windowElement.classList.add('llm-chat-launcher-dragging');
                windowElement.style.left = (startLeft + dx) + 'px';
                windowElement.style.top = (startTop + dy) + 'px';
                clampPosition();
                event.preventDefault();
            }

            function onPointerUp(event) {
                if (!dragging) {
                    return;
                }
                dragging = false;
                windowElement.classList.remove('llm-chat-launcher-dragging');
                if (button.releasePointerCapture && event.pointerId !== undefined) {
                    button.releasePointerCapture(event.pointerId);
                }
                if (moved) {
                    suppressClick = true;
                    savePosition();
                    event.preventDefault();
                }
            }

            button.addEventListener('pointerdown', onPointerDown);
            button.addEventListener('pointermove', onPointerMove);
            button.addEventListener('pointerup', onPointerUp);
            button.addEventListener('pointercancel', onPointerUp);
            button.addEventListener('click', function (event) {
                if (suppressClick) {
                    suppressClick = false;
                    event.preventDefault();
                    event.stopImmediatePropagation();
                }
            }, true);
            window.addEventListener('resize', clampPosition);

            restorePosition();
        }

        attach(0);
    };
};
