# Mini-Ledger API

API REST para transferências financeiras com integridade e idempotência.

## Decisões Técnicas

### 1. Por que `BigDecimal` e não `double`?
O `double` usa ponto flutuante binário, que não consegue representar frações decimais exatas. Por exemplo, `0.1 + 0.2` em `double` pode resultar em `0.30000000000000004`. Em um sistema financeiro, isso corromperia os saldos. O `BigDecimal` garante precisão exata e nos permite validar a escala (rejeitando `10.999` com `amount.scale() != 2`).

### 2. Como garanti a Atomicidade?
Uso a anotação `@Transactional` no método `executeTransfer` da camada `TransferService`. O isolamento padrão do Spring garante que o débito, o crédito e a criação do registro da transferência ocorram na mesma transação. Se qualquer exceção não-checada for lançada, o Spring faz o rollback automático de todas as operações no banco.

### 3. Como garanti a Idempotência?
Utilizo duas barreiras:
1. **Na aplicação:** O `TransferService` faz uma busca por `findByIdempotencyKey` antes de processar.
2. **No banco de dados:** A entidade `Transfer` possui `@Column(unique = true)` no `idempotencyKey`. Se duas requisições passarem pela primeira barreira simultaneamente, o banco vai rejeitar a segunda inserção com uma `DataIntegrityViolationException`, provocando rollback e garantindo uma única transferência.

### 4. Como garanti a Consistência sob Concorrência?
Escolhi o **Lock Pessimista** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) no `AccountRepository.findByIdForUpdate`. 
*   **O Trade-off:** O lock otimista (`@Version`) exigiria retentativas (retries) da aplicação em caso de colisão. Como o cenário de teste exigia 100 transferências paralelas entre as mesmas duas contas, o lock pessimista serializa o acesso e evita que a aplicação tenha que lidar com falhas de versão.
*   **Prevenção de Deadlock:** Antes de dar o lock nas contas, o código sempre ordena os IDs (`Math.min` e `Math.max`), garantindo que a conta A sempre tranque antes da B (ou vice-versa), evitando deadlocks clássicos.

### 5. HTTP Status Retornados
*   `201 Created`: Transferência concluída.
*   `400 Bad Request`: Erros de validação (escala errada, mesma conta, header ausente).
*   `422 Unprocessable Entity`: Regra de negócio violada (saldo insuficiente).
*   `500 Internal Server Error`: Falhas inesperadas.
