package com.example.miniledger;

import com.example.miniledger.model.Account;
import com.example.miniledger.repository.AccountRepository;
import com.example.miniledger.repository.TransferRepository;
import com.example.miniledger.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000"
})
class TransferIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accountRepo;
    @Autowired TransferRepository transferRepo;
    @Autowired TransferService transferService;

    @BeforeEach
    void clean() {
        transferRepo.deleteAll();
        accountRepo.deleteAll();
    }

    @Test
    void successfulTransferUpdatesBalancesAndCreatesTransfer() throws Exception {
        Account alice = accountRepo.save(new Account("Alice", new BigDecimal("1000.00")));
        Account bob   = accountRepo.save(new Account("Bob",   new BigDecimal("0.00")));

        String body = String.format(
                "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":100.00}", alice.getId(), bob.getId());

        mvc.perform(post("/transfers")
                        .header("Idempotency-Key", "it-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        assertThat(accountRepo.findById(alice.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("900.00");
        assertThat(accountRepo.findById(bob.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(transferRepo.count()).isEqualTo(1);
    }

    @Test
    void concurrentTransfersKeepBalancesConsistent() throws Exception {
        Account alice = accountRepo.save(new Account("Alice", new BigDecimal("1000.00")));
        Account bob   = accountRepo.save(new Account("Bob",   new BigDecimal("0.00")));

        final int N = 100;
        final BigDecimal perTransfer = new BigDecimal("10.00");
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                startGate.await();
                transferService.executeTransfer(alice.getId(), bob.getId(), perTransfer, "conc-" + idx);
                return null;
            }));
        }
        startGate.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        Account a = accountRepo.findById(alice.getId()).orElseThrow();
        Account b = accountRepo.findById(bob.getId()).orElseThrow();

        assertThat(a.getBalance()).isEqualByComparingTo("0.00");
        assertThat(b.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(a.getBalance().signum()).isGreaterThanOrEqualTo(0);
        assertThat(b.getBalance().signum()).isGreaterThanOrEqualTo(0);
        assertThat(transferRepo.count()).isEqualTo(N);
    }

    @Test
    void sameIdempotencyKeyReturnsSameBodyAndCreatesOneTransfer() throws Exception {
        Account alice = accountRepo.save(new Account("Alice", new BigDecimal("1000.00")));
        Account bob   = accountRepo.save(new Account("Bob",   new BigDecimal("0.00")));

        String body = String.format(
                "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":100.00}", alice.getId(), bob.getId());

        MvcResult r1 = mvc.perform(post("/transfers")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        MvcResult r2 = mvc.perform(post("/transfers")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        String json1 = r1.getResponse().getContentAsString();
        String json2 = r2.getResponse().getContentAsString();

        // Substitui o valor do createdAt por um texto fixo.
        // Isso evita a falha porque o H2 trunca os nanossegundos ao salvar no banco,
        // fazendo a string original ter mais casas decimais que a string lida do banco.
        String normalized1 = json1.replaceAll("\"createdAt\":\"[^\"]+\"", "\"createdAt\":\"TRUNCATED\"");
        String normalized2 = json2.replaceAll("\"createdAt\":\"[^\"]+\"", "\"createdAt\":\"TRUNCATED\"");

        // Compara o corpo JSON exato (ignorando apenas a precisão do timestamp)
        assertThat(normalized2).isEqualTo(normalized1);
        
        // Verifica que apenas 1 transferência foi criada
        assertThat(transferRepo.count()).isEqualTo(1);

        // Verifica que o débito aconteceu uma única vez
        assertThat(accountRepo.findById(alice.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("900.00");
        assertThat(accountRepo.findById(bob.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
    }
}
