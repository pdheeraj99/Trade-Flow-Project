package com.tradeflow.wallet;

import com.tradeflow.common.dto.WalletBalanceDTO;
import com.tradeflow.wallet.entity.Wallet;
import com.tradeflow.wallet.exception.FaucetCooldownException;
import com.tradeflow.wallet.exception.InsufficientFundsException;
import com.tradeflow.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for WalletService using Testcontainers
 */
@SpringBootTest
@Testcontainers
class WalletServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("tradeflow_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-test-schema.sql");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Disable RabbitMQ for this test
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration");
    }

    @Autowired
    private WalletService walletService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        // Clear Redis keys for test isolation
        redisTemplate.delete(redisTemplate.keys("faucet:cooldown:*"));
        redisTemplate.delete(redisTemplate.keys("processed:saga:*"));
    }

    @Test
    @DisplayName("Should create wallet with zero balance")
    void shouldCreateWalletWithZeroBalance() {
        // When
        Wallet wallet = walletService.getOrCreateWallet(testUserId, "USD");

        // Then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getWalletId()).isNotNull();
        assertThat(wallet.getUserId()).isEqualTo(testUserId);
        assertThat(wallet.getCurrency()).isEqualTo("USD");

        WalletBalanceDTO balance = walletService.getBalance(testUserId, "USD");
        assertThat(balance.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should claim faucet and receive $10,000")
    void shouldClaimFaucetSuccessfully() {
        // When
        WalletBalanceDTO result = walletService.claimFaucet(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(result.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should enforce faucet cooldown")
    void shouldEnforceFaucetCooldown() {
        // Given - First claim
        walletService.claimFaucet(testUserId);

        // When/Then - Second claim should fail
        assertThatThrownBy(() -> walletService.claimFaucet(testUserId))
                .isInstanceOf(FaucetCooldownException.class);
    }

    @Test
    @DisplayName("Should reserve funds successfully")
    void shouldReserveFundsSuccessfully() {
        // Given
        walletService.claimFaucet(testUserId);
        UUID orderId = UUID.randomUUID();

        // When
        walletService.reserveFunds(testUserId, "USD", new BigDecimal("1000.00"), orderId);

        // Then
        WalletBalanceDTO balance = walletService.getBalance(testUserId, "USD");
        assertThat(balance.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(balance.getReservedBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should throw exception when reserving more than available")
    void shouldThrowExceptionWhenInsufficientFunds() {
        // Given
        walletService.claimFaucet(testUserId);
        UUID orderId = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() -> walletService.reserveFunds(testUserId, "USD", new BigDecimal("50000.00"), orderId))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("Should release reserved funds")
    void shouldReleaseFundsSuccessfully() {
        // Given
        walletService.claimFaucet(testUserId);
        UUID orderId = UUID.randomUUID();
        walletService.reserveFunds(testUserId, "USD", new BigDecimal("1000.00"), orderId);

        // When
        walletService.releaseFunds(testUserId, "USD", new BigDecimal("1000.00"), orderId);

        // Then
        WalletBalanceDTO balance = walletService.getBalance(testUserId, "USD");
        assertThat(balance.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(balance.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should get all balances for user")
    void shouldGetAllBalancesForUser() {
        // Given
        walletService.getOrCreateWallet(testUserId, "USD");
        walletService.getOrCreateWallet(testUserId, "BTC");
        walletService.getOrCreateWallet(testUserId, "ETH");

        // When
        List<WalletBalanceDTO> balances = walletService.getBalances(testUserId);

        // Then
        assertThat(balances).hasSize(3);
        assertThat(balances).extracting(WalletBalanceDTO::getCurrency)
                .containsExactlyInAnyOrder("USD", "BTC", "ETH");
    }

    @Test
    @DisplayName("Should handle concurrent reservations with pessimistic locking")
    void shouldHandleConcurrentReservations() throws InterruptedException {
        // Given
        walletService.claimFaucet(testUserId);
        int numThreads = 10;
        BigDecimal reserveAmount = new BigDecimal("500.00");

        // When - 10 threads try to reserve $500 each (total $5000)
        Thread[] threads = new Thread[numThreads];
        int[] successCount = { 0 };
        int[] failCount = { 0 };

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    walletService.reserveFunds(testUserId, "USD", reserveAmount, UUID.randomUUID());
                    synchronized (successCount) {
                        successCount[0]++;
                    }
                } catch (InsufficientFundsException e) {
                    synchronized (failCount) {
                        failCount[0]++;
                    }
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All should succeed as total is $5000 (less than $10000)
        WalletBalanceDTO balance = walletService.getBalance(testUserId, "USD");
        assertThat(balance.getAvailableBalance().add(balance.getReservedBalance()))
                .isEqualByComparingTo(new BigDecimal("10000.00"));
    }
}
