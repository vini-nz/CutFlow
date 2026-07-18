# Changelog

## [0.1.0] — MVP

- Cadastro de Projeto, Chapa (por espessura) e Peça
- Algoritmo Guillotine de otimização (`OtimizadorDePlano` / `GuillotineOtimizadorDePlano`), isolado e testado (`GuillotineOtimizadorDePlanoTest`)
- Orquestração por espessura no `PlanoDeCorteService`, com numeração global de chapas e etiquetas
- Registro de sobras (`Sobra`) ao final do encaixe de cada chapa
- Exportação em PDF (`PlanoDeCortePdfService`, OpenPDF)
- Frontend PWA (React + Vite + Tailwind): lista de projetos, workspace de projeto com formulários de chapa/peça, visualização Canvas do plano gerado, exportação de PDF
- Schema PostgreSQL versionado (`db/cutflow_ddl.sql`) com projeto de demonstração (`db/seed.sql`)
- Ambiente containerizado com Docker Compose
