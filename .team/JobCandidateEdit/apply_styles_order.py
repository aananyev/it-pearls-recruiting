#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Порядок слоёв SCSS по контракту 6.4 (дизайн 2.3):
screen-specific (job-candidate-editor) подключается ПОСЛЕ edit-screen-shared-styles."""
import io, sys

# ---------- 1. modern-темы: перенос import/include из позиции ДО shared в ПОСЛЕ ----------
MODERN = {
    'hunttech-modern': 'hunttech-modern',
    'hunttech-modern-light': 'hunttech-modern-light',
    'hunttech-modern-dark': 'hunttech-modern-dark',
}

for theme, mixin in MODERN.items():
    p = 'modules/web/themes/%s/styles.scss' % theme
    with io.open(p, encoding='utf-8') as f:
        s = f.read()

    old_imp = '@import "com.company.hunttech/chat-style.css";\n@import "com.company.hunttech/job-candidate-editor.scss";\n@import "com.company.hunttech/user-ai-profile";'
    new_imp = '@import "com.company.hunttech/chat-style.css";\n@import "com.company.hunttech/user-ai-profile";'
    assert s.count(old_imp) == 1, theme + ' import-блок 1'
    s = s.replace(old_imp, new_imp)

    old_imp2 = '@import "com.company.hunttech/edit-screen-shared-styles";'
    new_imp2 = '@import "com.company.hunttech/edit-screen-shared-styles";\n@import "com.company.hunttech/job-candidate-editor.scss";'
    assert s.count(old_imp2) == 1, theme + ' import shared'
    s = s.replace(old_imp2, new_imp2)

    old_inc = '  @include com_company_hunttech-%s-ext;\n  @include job-candidate-editor-theme;\n  @include user-ai-profile;' % theme
    new_inc = '  @include com_company_hunttech-%s-ext;\n  @include user-ai-profile;' % theme
    assert s.count(old_inc) == 1, theme + ' include-блок'
    s = s.replace(old_inc, new_inc)

    old_inc2 = '  @include edit-screen-shared-styles;'
    new_inc2 = '  @include edit-screen-shared-styles;\n  @include job-candidate-editor-theme;'
    assert s.count(old_inc2) == 1, theme + ' include shared'
    s = s.replace(old_inc2, new_inc2)

    with io.open(p, 'w', encoding='utf-8') as f:
        f.write(s)
    print('OK modern:', p)

# ---------- 2. halo/havana/helium/hover: вынос из -ext в styles.scss ----------
EXT_THEMES = ['halo', 'havana', 'helium', 'hover']

for theme in EXT_THEMES:
    # 2a. -ext.scss: удалить import и include
    ep = 'modules/web/themes/%s/com.company.hunttech/%s-ext.scss' % (theme, theme)
    with io.open(ep, encoding='utf-8') as f:
        s = f.read()

    old_imp = '@import "job-candidate-editor.scss";\n\n/* Define your theme modifications inside next mixin */'
    new_imp = '/* Define your theme modifications inside next mixin */'
    assert s.count(old_imp) == 1, theme + '-ext import'
    s = s.replace(old_imp, new_imp)

    old_inc = '@mixin com_company_hunttech-%s-ext {\n    @include job-candidate-editor-theme;\n' % theme
    new_inc = '@mixin com_company_hunttech-%s-ext {\n' % theme
    assert s.count(old_inc) == 1, theme + '-ext include'
    s = s.replace(old_inc, new_inc)

    with io.open(ep, 'w', encoding='utf-8') as f:
        f.write(s)
    print('OK ext:', ep)

    # 2b. styles.scss: добавить import/include ПОСЛЕ shared
    sp = 'modules/web/themes/%s/styles.scss' % theme
    with io.open(sp, encoding='utf-8') as f:
        s = f.read()

    old_imp2 = '@import "com.company.hunttech/edit-screen-shared-styles";'
    new_imp2 = '@import "com.company.hunttech/edit-screen-shared-styles";\n@import "com.company.hunttech/job-candidate-editor.scss";'
    assert s.count(old_imp2) == 1, theme + ' styles import shared'
    s = s.replace(old_imp2, new_imp2)

    old_inc2 = '  @include edit-screen-shared-styles;'
    new_inc2 = '  @include edit-screen-shared-styles;\n  @include job-candidate-editor-theme;'
    assert s.count(old_inc2) == 1, theme + ' styles include shared'
    s = s.replace(old_inc2, new_inc2)

    with io.open(sp, 'w', encoding='utf-8') as f:
        f.write(s)
    print('OK styles:', sp)

print('Все 7 тем: screen-specific слой перенесён после shared.')
