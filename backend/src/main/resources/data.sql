-- ============================================================================
-- Empresas (tenants). Renomeie para o nome real de cada empresa quando for
-- usar de verdade, por exemplo:
--   UPDATE empresas SET nome = 'Nome Real da Empresa' WHERE id = 1;
-- ============================================================================
INSERT IGNORE INTO empresas (id, nome, ativa) VALUES
(1, 'Empresa Demonstração 1', true),
(2, 'Empresa Demonstração 2', true);

-- ============================================================================
-- Usuários administradores padrão — um por empresa.
-- Login (usuário / senha):
--   empresa 1  ->  admin1 / admin123
--   empresa 2  ->  admin2 / admin123
-- >>> TROQUE ESSAS SENHAS NO PRIMEIRO ACESSO <<<
-- ============================================================================
INSERT IGNORE INTO usuarios (id, nome, usuario, senha, papel, ativo, empresa_id) VALUES
(1, 'Administrador', 'admin1', '$2b$10$9gL7XetEObmG7k.L53fj.OsKZ/hgaAiyGZrINxcjhgGixr8jgH73C', 'ADMIN', true, 1),
(2, 'Administrador', 'admin2', '$2b$10$9gL7XetEObmG7k.L53fj.OsKZ/hgaAiyGZrINxcjhgGixr8jgH73C', 'ADMIN', true, 2);

-- ============================================================================
-- Dados de exemplo — EMPRESA 1
-- ============================================================================
INSERT IGNORE INTO categorias (id, nome, descricao, empresa_id) VALUES
(1, 'Material de Escritório', 'Papelaria e suprimentos administrativos', 1),
(2, 'Limpeza', 'Produtos de limpeza e higiene', 1),
(3, 'Ferramentas', 'Ferramentas manuais e elétricas', 1),
(4, 'EPI', 'Equipamentos de Proteção Individual', 1);

INSERT IGNORE INTO fornecedores (id, nome, cnpj, telefone, email, endereco, empresa_id) VALUES
(1, 'Papelaria Central Ltda', '12.345.678/0001-90', '(41) 3222-1000', 'contato@papelariacentral.com.br', 'Rua das Flores, 100 - Curitiba/PR', 1),
(2, 'Distribuidora Limpa Tudo', '98.765.432/0001-10', '(41) 3333-2000', 'vendas@limpatudo.com.br', 'Av. Industrial, 500 - Curitiba/PR', 1),
(3, 'Ferragens Sul', '11.222.333/0001-44', '(41) 3444-5000', 'comercial@ferragensul.com.br', 'Rua dos Metalúrgicos, 220 - Curitiba/PR', 1);

INSERT IGNORE INTO fornecedores (id, nome, cnpj, telefone, email, endereco, empresa_id) VALUES
(6, 'Suprimentos Brasil Ltda', '44.555.666/0001-77', '(41) 3555-7000', 'vendas@suprimentosbrasil.com.br', 'Rua do Comércio, 80 - Curitiba/PR', 1);

-- Cada linha de "materiais" é uma variação (marca/modelo/C.A.) de um produto. O código é do produto:
-- MAT-002 aparece duas vezes, com marcas diferentes (id 2 e id 10).
INSERT IGNORE INTO materiais (id, codigo, nome, descricao, marca, modelo, ca, unidade, quantidade_atual, quantidade_minima, preco_unitario, localizacao, data_cadastro, ativo, categoria_id, empresa_id) VALUES
(1, 'MAT-001', 'Papel A4 (resma 500 folhas)', 'Papel sulfite branco 75g/m²', '', '', '', 'RESMA', 45, 20, 24.90, 'Prateleira A1', NOW(), true, 1, 1),
(2, 'MAT-002', 'Caneta esferográfica azul', 'Caixa com 50 unidades', 'Marca A', '', '', 'CAIXA', 8, 10, 32.50, 'Prateleira A2', NOW(), true, 1, 1),
(3, 'MAT-003', 'Álcool em gel 70%', 'Frasco de 500ml', '', '', '', 'FRASCO', 60, 15, 9.90, 'Prateleira B1', NOW(), true, 2, 1),
(4, 'MAT-004', 'Detergente neutro', 'Frasco de 5 litros', '', '', '', 'FRASCO', 5, 8, 18.00, 'Prateleira B2', NOW(), true, 2, 1),
(5, 'MAT-005', 'Furadeira de impacto', 'Furadeira elétrica 650W', '', '', '', 'UNIDADE', 3, 2, 289.90, 'Armário C1', NOW(), true, 3, 1),
(6, 'MAT-006', 'Luva de segurança', 'Par de luvas de raspa de couro', '', '', '', 'PAR', 12, 20, 15.75, 'Armário D1', NOW(), true, 4, 1),
(7, 'MAT-007', 'Capacete de segurança', 'Capacete classe B branco', '', '', '', 'UNIDADE', 25, 10, 42.00, 'Armário D2', NOW(), true, 4, 1),
(10, 'MAT-002', 'Caneta esferográfica azul', 'Caixa com 50 unidades', 'Marca B', '', '', 'CAIXA', 15, 10, 29.90, 'Prateleira A2', NOW(), true, 1, 1);

-- Fornecedores de cada variação, com o preço de cada um.
INSERT IGNORE INTO material_fornecedores (id, material_id, fornecedor_id, preco_unitario, data_atualizacao, empresa_id) VALUES
(1, 1, 1, 24.90, NOW(), 1),
(2, 2, 1, 32.50, NOW(), 1),
(3, 3, 2, 9.90, NOW(), 1),
(4, 4, 2, 18.00, NOW(), 1),
(5, 5, 3, 289.90, NOW(), 1),
(6, 6, 3, 15.75, NOW(), 1),
(7, 7, 3, 42.00, NOW(), 1),
(8, 10, 1, 30.50, NOW(), 1),
(9, 10, 6, 29.90, NOW(), 1);

INSERT IGNORE INTO movimentacoes (id, material_id, tipo, quantidade, responsavel, observacao, data_movimentacao, empresa_id) VALUES
(1, 1, 'ENTRADA', 50, 'João Silva', 'Compra mensal', DATE_SUB(NOW(), INTERVAL 10 DAY), 1),
(2, 1, 'SAIDA', 5, 'Maria Souza', 'Uso setor financeiro', DATE_SUB(NOW(), INTERVAL 3 DAY), 1),
(3, 3, 'ENTRADA', 40, 'João Silva', 'Reposição de estoque', DATE_SUB(NOW(), INTERVAL 7 DAY), 1),
(4, 6, 'AVARIA', 2, 'Carlos Lima', 'Luvas rasgadas encontradas na conferência de estoque', DATE_SUB(NOW(), INTERVAL 1 DAY), 1);

-- ============================================================================
-- Dados de exemplo — EMPRESA 2 (demonstra que os dados ficam separados)
-- ============================================================================
INSERT IGNORE INTO categorias (id, nome, descricao, empresa_id) VALUES
(5, 'Informática', 'Periféricos e suprimentos de TI', 2),
(6, 'Embalagens', 'Caixas e materiais de embalagem', 2);

INSERT IGNORE INTO fornecedores (id, nome, cnpj, telefone, email, endereco, empresa_id) VALUES
(4, 'TechSupply Distribuidora', '22.333.444/0001-55', '(11) 4000-1000', 'vendas@techsupply.com.br', 'Av. Paulista, 1200 - São Paulo/SP', 2),
(5, 'Embalagens Rápidas Ltda', '33.444.555/0001-66', '(11) 4000-2000', 'contato@embalagensrapidas.com.br', 'Rua do Comércio, 45 - São Paulo/SP', 2);

INSERT IGNORE INTO materiais (id, codigo, nome, descricao, marca, modelo, ca, unidade, quantidade_atual, quantidade_minima, preco_unitario, localizacao, data_cadastro, ativo, categoria_id, empresa_id) VALUES
(8, 'MAT-001', 'Mouse óptico USB', 'Mouse óptico com fio, 1000 DPI', '', '', '', 'UNIDADE', 18, 10, 22.00, 'Prateleira 1', NOW(), true, 5, 2),
(9, 'MAT-002', 'Caixa de papelão média', 'Caixa 40x30x30cm', '', '', '', 'UNIDADE', 6, 15, 4.50, 'Estoque 2', NOW(), true, 6, 2);

INSERT IGNORE INTO material_fornecedores (id, material_id, fornecedor_id, preco_unitario, data_atualizacao, empresa_id) VALUES
(10, 8, 4, 22.00, NOW(), 2),
(11, 9, 5, 4.50, NOW(), 2);

INSERT IGNORE INTO movimentacoes (id, material_id, tipo, quantidade, responsavel, observacao, data_movimentacao, empresa_id) VALUES
(5, 8, 'ENTRADA', 20, 'Ana Paula', 'Compra inicial', DATE_SUB(NOW(), INTERVAL 5 DAY), 2),
(6, 9, 'SAIDA', 9, 'Rafael Nunes', 'Envio de pedidos', DATE_SUB(NOW(), INTERVAL 2 DAY), 2);
