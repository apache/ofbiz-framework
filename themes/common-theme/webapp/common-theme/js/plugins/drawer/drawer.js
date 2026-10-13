(function ($) {
    const SKELETON_HTML = `
  <div class="drawer-skeleton">
    <div class="sk-badges">
      <div class="sk sk-badge" style="width:110px"></div>
      <div class="sk sk-badge" style="width:95px"></div>
    </div>
    <div class="sk-row">
      <div class="sk sk-title-line"></div>
      <div class="sk sk-link"></div>
    </div>
    ${[55, 70, 90, 60, 75].map(w => `
      <div class="sk-card">
        <div class="sk-card-content">
          <div class="sk sk-card-title" style="width:${w}%"></div>
          <div class="sk sk-card-sub"></div>
        </div>
        <div class="sk-chevron"></div>
      </div>
    `).join('')}
  </div>`;

    function initDrawer() {
        if ($('#app-drawer').length) return;
        $('body').append(`
          <div id="app-drawer" role="dialog" aria-modal="false">
            <div class="drawer-header">
              <span class="drawer-title"></span>
              <button class="drawer-close" aria-label="Fermer"></button>
            </div>
            <div class="drawer-body"></div>
          </div>
        `);
    }

    function openDrawer(options) {
        const $drawer = $('#app-drawer');
        const $body = $drawer.find('.drawer-body');

        $drawer.find('.drawer-title').text(options.title || 'Détail');
        if (options.width) {
            $drawer.css('width', parseInt(options.width, 10) + 'px');
        } else {
            $drawer.css('width', '');
        }

        $body.html(SKELETON_HTML);
        $drawer.addClass('is-open');

        const ajaxOptions = {
            url: options.url,
            method: 'GET',
            success: function (html) {
                $body.html(html);
            },
            error: function () {
                $body.html('<p class="drawer-error">Erreur lors du chargement.</p>');
            }
        };

        if (options.params && Object.keys(options.params).length) {
            ajaxOptions.method = 'POST';
            ajaxOptions.data = options.params;
        }

        $.ajax(ajaxOptions);
    }

    function closeDrawer() {
        $('#app-drawer').removeClass('is-open');
    }

    function parseParams(raw) {
        if (!raw) return {};
        try {
            return JSON.parse(raw);
        } catch (e) {
            return {};
        }
    }

    $(function () {
        initDrawer();

        $(document).on('click', '[data-open-in="drawer"]', function (e) {
            e.preventDefault();
            const $el = $(this);

            openDrawer({
                url: $el.data('dialog-url') || $el.attr('href'),
                title: $el.data('dialog-title') || $el.attr('title') || 'Détail',
                width: $el.data('dialog-width'),
                params: parseParams($el.attr('data-dialog-params'))
            });
        });

        $(document).on('click', '.drawer-close', closeDrawer);
        $(document).on('keydown', function (e) {
            if (e.key === 'Escape') closeDrawer();
        });
    });
}(jQuery));