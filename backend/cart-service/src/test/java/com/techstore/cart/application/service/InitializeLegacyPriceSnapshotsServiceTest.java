package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.CartStorePort.LegacyCartItem;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.port.out.ProductCatalogPort.ProductSummary;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeLegacyPriceSnapshotsServiceTest {
    private static final UUID OWNER_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID OWNER_B = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock private CartStorePort cartStore;
    @Mock private ProductCatalogPort productCatalog;

    @Test
    void snapshotsActiveProductOnceForAllPendingOwnerLines() {
        when(cartStore.findPendingPriceSnapshots()).thenReturn(List.of(
                new LegacyCartItem(OWNER_A, PRODUCT_ID, 2),
                new LegacyCartItem(OWNER_B, PRODUCT_ID, 4)));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct()));
        when(cartStore.resolvePendingPriceSnapshot(OWNER_A, PRODUCT_ID,
                UnitPriceSnapshot.known(new BigDecimal("49.90")))).thenReturn(true);
        when(cartStore.resolvePendingPriceSnapshot(OWNER_B, PRODUCT_ID,
                UnitPriceSnapshot.known(new BigDecimal("49.90")))).thenReturn(true);

        service().initialize();

        verify(productCatalog, times(1)).findActiveById(PRODUCT_ID);
        verify(cartStore).resolvePendingPriceSnapshot(OWNER_A, PRODUCT_ID,
                UnitPriceSnapshot.known(new BigDecimal("49.90")));
        verify(cartStore).resolvePendingPriceSnapshot(OWNER_B, PRODUCT_ID,
                UnitPriceSnapshot.known(new BigDecimal("49.90")));
        verify(cartStore, never()).remove(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retainsMissingOrInactiveProductAsUnknownWithoutPrice() {
        when(cartStore.findPendingPriceSnapshots()).thenReturn(List.of(
                new LegacyCartItem(OWNER_A, PRODUCT_ID, 2)));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(cartStore.resolvePendingPriceSnapshot(OWNER_A, PRODUCT_ID,
                UnitPriceSnapshot.unknown())).thenReturn(true);

        service().initialize();

        verify(cartStore).resolvePendingPriceSnapshot(OWNER_A, PRODUCT_ID, UnitPriceSnapshot.unknown());
        verify(cartStore, never()).remove(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void leavesPendingRowsUnresolvedWhenCatalogIsUnavailable() {
        when(cartStore.findPendingPriceSnapshots()).thenReturn(List.of(
                new LegacyCartItem(OWNER_A, PRODUCT_ID, 2)));
        when(productCatalog.findActiveById(PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class, () -> service().initialize());

        verify(cartStore, never()).resolvePendingPriceSnapshot(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retryIsIdempotentAfterPendingRowsHaveBeenResolved() {
        when(cartStore.findPendingPriceSnapshots()).thenReturn(List.of(
                new LegacyCartItem(OWNER_A, PRODUCT_ID, 2)), List.of());
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct()));
        when(cartStore.resolvePendingPriceSnapshot(OWNER_A, PRODUCT_ID,
                UnitPriceSnapshot.known(new BigDecimal("49.90")))).thenReturn(true);

        service().initialize();
        service().initialize();

        verify(productCatalog, times(1)).findActiveById(PRODUCT_ID);
        verify(cartStore, times(1)).resolvePendingPriceSnapshot(OWNER_A, PRODUCT_ID,
                UnitPriceSnapshot.known(new BigDecimal("49.90")));
    }

    private InitializeLegacyPriceSnapshotsService service() {
        return new InitializeLegacyPriceSnapshotsService(cartStore, productCatalog);
    }

    private static ProductSummary activeProduct() {
        return new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null, 10);
    }
}
