(add-hook
  'nxml-mode-hook
  (lambda ()
    (setq rng-schema-locating-files-default
          (append '("/Users/keith/work/docbook-dev/xsl/locatingrules.xml")
                  rng-schema-locating-files-default ))))
