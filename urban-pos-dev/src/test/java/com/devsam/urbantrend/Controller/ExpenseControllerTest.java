package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ExpenseSummaryResponse;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;

    @Test
    void summaryCoversEveryExpenseNotJustTheVisiblePage() {
        when(expenseRepository.sumAll()).thenReturn(new BigDecimal("125000.50"));
        when(expenseRepository.countByDeletedAtIsNull()).thenReturn(431L);

        ExpenseSummaryResponse summary = new ExpenseController(expenseRepository, userRepository).summary();

        assertThat(summary.total()).isEqualByComparingTo("125000.50");
        assertThat(summary.count()).isEqualTo(431L);
    }
}
