/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.fcrepo.client.DeleteBuilder;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.HeadBuilder;
import org.fcrepo.client.PostBuilder;
import org.fcrepo.client.PutBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Aaron Coburn
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FcrepoTransactionManagerTest {

    @Mock
    private FcrepoClient mockClient;

    @Mock
    private PostBuilder mockPostBuilder, mockPostBuilder2;

    @Mock
    private PutBuilder mockPutBuilder;

    @Mock
    private DeleteBuilder mockDeleteBuilder;

    @Mock
    private GetBuilder mockGetBuilder;

    @Mock
    private HeadBuilder mockHeadBuilder;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(mockClient.head(any(URI.class))).thenReturn(mockHeadBuilder);
        when(mockClient.get(any(URI.class))).thenReturn(mockGetBuilder);
        when(mockGetBuilder.accept(any(String.class))).thenReturn(mockGetBuilder);
        when(mockGetBuilder.preferRepresentation(any(List.class), any(List.class))).thenReturn(mockGetBuilder);
        when(mockClient.post(any(URI.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.body(nullable(InputStream.class), nullable(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder2.body(nullable(InputStream.class), nullable(String.class))).thenReturn(mockPostBuilder2);
    }

    @Test
    public void testProperties() {
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        final String baseUrl = "http://localhost:8080/rest";
        final String authUsername = "foo";
        final String authPassword = "bar";
        final String authHost = "baz";
        final String transactionId = "1234567890";

        assertNull(txMgr.getAuthUsername());
        assertNull(txMgr.getAuthPassword());
        assertNull(txMgr.getAuthHost());
        assertNull(txMgr.getBaseUrl());

        txMgr.setBaseUrl(baseUrl);
        txMgr.setAuthUsername(authUsername);
        txMgr.setAuthPassword(authPassword);
        txMgr.setAuthHost(authHost);

        assertEquals(baseUrl, txMgr.getBaseUrl());
        assertEquals(authUsername, txMgr.getAuthUsername());
        assertEquals(authPassword, txMgr.getAuthPassword());
        assertEquals(authHost, txMgr.getAuthHost());
    }

    @Test
    public void testTransactionCommit() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String txUri = baseUrl + FcrepoConstants.TRANSACTION + "/1234567890";
        final URI commitUri = URI.create(txUri);
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri))).thenReturn(mockPostBuilder);
        when(mockClient.put(eq(commitUri))).thenReturn(mockPutBuilder);
        when(mockPostBuilder.perform()).thenReturn(
                new FcrepoResponse(beginUri, 201, singletonMap("Location", singletonList(txUri)), null));
        when(mockPutBuilder.perform()).thenReturn(
                new FcrepoResponse(commitUri, 204, emptyMap(), null));

        final DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);
        final FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();

        assertEquals(txUri, txObj.getSessionId());
        assertFalse(status.isCompleted());

        txMgr.commit(status);

        assertNull(txObj.getSessionId());
        assertTrue(status.isCompleted());
    }

    @Test
    public void testTransactionRollback() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String txUri = baseUrl + FcrepoConstants.TRANSACTION + "/1234567890";
        final URI rollbackUri = URI.create(txUri);
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri))).thenReturn(mockPostBuilder);
        when(mockClient.delete(eq(rollbackUri))).thenReturn(mockDeleteBuilder);
        when(mockPostBuilder.perform()).thenReturn(
                new FcrepoResponse(beginUri, 201, singletonMap("Location", singletonList(txUri)), null));
        when(mockDeleteBuilder.perform()).thenReturn(
                new FcrepoResponse(rollbackUri, 204, emptyMap(), null));

        final DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);
        final FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();

        assertEquals(txUri, txObj.getSessionId());
        assertFalse(status.isCompleted());

        txMgr.rollback(status);

        assertNull(txObj.getSessionId());
        assertTrue(status.isCompleted());
    }

    @Test
    public void testTransactionBeginError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockPostBuilder.perform()).thenThrow(
                new FcrepoOperationFailedException(beginUri, 400, "Bad Request"));

        assertThrows(CannotCreateTransactionException.class, () -> txMgr.getTransaction(txDef));
    }

    @Test
    public void testTransactionBeginNoLocationError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockPostBuilder.perform()).thenReturn(
                new FcrepoResponse(beginUri, 201, emptyMap(), null));

        assertThrows(CannotCreateTransactionException.class, () -> txMgr.getTransaction(txDef));
    }

    @Test
    public void testTransactionNullResponseError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String tx = "tx:1234567890";
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockPostBuilder.perform()).thenReturn(null);

        assertThrows(CannotCreateTransactionException.class, () -> txMgr.getTransaction(txDef));
    }

    @Test
    public void testTransactionCommitError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String txUri = baseUrl + FcrepoConstants.TRANSACTION + "/1234567890";
        final URI commitUri = URI.create(txUri);
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri))).thenReturn(mockPostBuilder);
        when(mockClient.put(eq(commitUri))).thenReturn(mockPutBuilder);
        when(mockPostBuilder.perform()).thenReturn(
                new FcrepoResponse(beginUri, 201, singletonMap("Location", singletonList(txUri)), null));
        when(mockPutBuilder.perform()).thenThrow(
                new FcrepoOperationFailedException(commitUri, 400, "Bad Request"));

        final DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);

        final FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();
        assertEquals(txUri, txObj.getSessionId());
        assertFalse(status.isCompleted());

        assertThrows(TransactionSystemException.class, () -> txMgr.commit(status));
    }

    @Test
    public void testTransactionRollbackError() throws FcrepoOperationFailedException {
        final String baseUrl = "http://localhost:8080/rest";
        final String txUri = baseUrl + FcrepoConstants.TRANSACTION + "/1234567890";
        final URI rollbackUri = URI.create(txUri);
        final URI beginUri = URI.create(baseUrl + FcrepoConstants.TRANSACTION);
        final FcrepoTransactionManager txMgr = new FcrepoTransactionManager();
        txMgr.setBaseUrl(baseUrl);
        TestUtils.setField(txMgr, "fcrepoClient", mockClient);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
        final DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.afterPropertiesSet();

        when(mockClient.post(eq(beginUri))).thenReturn(mockPostBuilder);
        when(mockClient.delete(eq(rollbackUri))).thenReturn(mockDeleteBuilder);
        when(mockPostBuilder.perform()).thenReturn(
                new FcrepoResponse(beginUri, 201, singletonMap("Location", singletonList(txUri)), null));
        when(mockDeleteBuilder.perform()).thenThrow(
                new FcrepoOperationFailedException(rollbackUri, 400, "Bad Request"));

        final DefaultTransactionStatus status = (DefaultTransactionStatus)txMgr.getTransaction(txDef);

        final FcrepoTransactionObject txObj = (FcrepoTransactionObject)status.getTransaction();
        assertEquals(txUri, txObj.getSessionId());
        assertFalse(status.isCompleted());

        assertThrows(TransactionSystemException.class, () -> txMgr.rollback(status));
    }
}
