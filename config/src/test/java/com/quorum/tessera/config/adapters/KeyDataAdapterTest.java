package com.quorum.tessera.config.adapters;

import com.quorum.tessera.config.KeyData;
import com.quorum.tessera.config.KeyDataConfig;
import com.quorum.tessera.config.PrivateKeyData;
import com.quorum.tessera.config.keypairs.*;
import org.junit.Test;

import java.nio.file.Path;

import static com.quorum.tessera.config.PrivateKeyType.UNLOCKED;
import com.quorum.tessera.config.keys.KeyEncryptor;
import com.quorum.tessera.config.keys.KeyEncryptorHolder;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import org.junit.Before;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeyDataAdapterTest {

    private KeyDataAdapter adapter = new KeyDataAdapter();

    private KeyEncryptor keyEncryptor = mock(KeyEncryptor.class);

    @Before
    public void onSetUp() {
        KeyEncryptorHolder.INSTANCE.setKeyEncryptor(keyEncryptor);
        adapter = new KeyDataAdapter();
    }

    @Test
    public void marshallDirectKeys() {
        final ConfigKeyPair keys = new DirectKeyPair("PUB", "PRIV");
        final KeyData expected = new KeyData();
        expected.setPublicKey("PUB");
        expected.setPrivateKey("PRIV");

        final KeyData marshalledKey = adapter.marshal(keys);

        assertThat(marshalledKey).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void marshallInlineKeys() {
        final PrivateKeyData pkd = new PrivateKeyData("val", null, null, null, null);
        final ConfigKeyPair keys = new InlineKeypair("PUB", new KeyDataConfig(pkd, UNLOCKED), keyEncryptor);
        final KeyData expected = new KeyData();

        expected.setPublicKey("PUB");
        expected.setConfig(new KeyDataConfig(pkd, UNLOCKED));

        final KeyData marshalledKey = adapter.marshal(keys);

        assertThat(marshalledKey).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void marshallFilesystemKeys() {
        final Path path = mock(Path.class);

        KeyEncryptor keyEncryptor = mock(KeyEncryptor.class);

        final FilesystemKeyPair keyPair = new FilesystemKeyPair(path, path, keyEncryptor);

        final KeyData expected = new KeyData();
        expected.setPublicKeyPath(path);
        expected.setPrivateKeyPath(path);

        final KeyData result = adapter.marshal(keyPair);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void marshallAzureKeys() {
        final AzureVaultKeyPair keyPair = new AzureVaultKeyPair("pubId", "privId", "pubVer", "privVer");

        final KeyData expected = new KeyData();
        expected.setAzureVaultPublicKeyId("pubId");
        expected.setAzureVaultPrivateKeyId("privId");
        expected.setAzureVaultPublicKeyVersion("pubVer");
        expected.setAzureVaultPrivateKeyVersion("privVer");

        final KeyData result = adapter.marshal(keyPair);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void marshallHashicorpKeys() {
        final HashicorpVaultKeyPair keyPair =
                new HashicorpVaultKeyPair("pubId", "privId", "secretEngineName", "secretName", 0);

        final KeyData expected = new KeyData();
        expected.setHashicorpVaultPublicKeyId("pubId");
        expected.setHashicorpVaultPrivateKeyId("privId");
        expected.setHashicorpVaultSecretEngineName("secretEngineName");
        expected.setHashicorpVaultSecretName("secretName");

        final KeyData result = adapter.marshal(keyPair);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void marshallAWSKeys() {
        final AWSKeyPair keyPair = new AWSKeyPair("pubId", "privId");

        final KeyData expected = new KeyData();
        expected.setAwsSecretsManagerPublicKeyId("pubId");
        expected.setAwsSecretsManagerPrivateKeyId("privId");

        final KeyData result = adapter.marshal(keyPair);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void marshallUnsupportedKeys() {
        final KeyDataConfig keyDataConfig = mock(KeyDataConfig.class);
        final Path path = mock(Path.class);
        // set a random selection of values that are not sufficient to make a complete key pair of any type
        final UnsupportedKeyPair keyPair =
                new UnsupportedKeyPair(
                        keyDataConfig, "priv", null, path, null, null, null, null, null, null, null, null, null, null, null, null);

        final KeyData expected = new KeyData();
        expected.setConfig(keyDataConfig);
        expected.setPrivateKey("priv");
        expected.setPrivateKeyPath(path);

        final KeyData result = adapter.marshal(keyPair);

        assertThat(result).isEqualTo(expected);
    }

    class UnknownKeyPair implements ConfigKeyPair {

        @Override
        public String getPublicKey() {
            return null;
        }

        @Override
        public String getPrivateKey() {
            return null;
        }

        @Override
        public void withPassword(String password) {
            // do nothing
        }

        @Override
        public String getPassword() {
            return null;
        }
    }

    @Test
    public void marshallUnknownKeyPairType() {
        final ConfigKeyPair keyPair = new UnknownKeyPair();

        Throwable ex = catchThrowable(() -> adapter.marshal(keyPair));

        assertThat(ex).isInstanceOf(UnsupportedOperationException.class);
        assertThat(ex).hasMessage("The keypair type " + keyPair.getClass() + " is not allowed");
    }

    @Test
    public void marshallLockedKeyNullifiesPrivateKey() {
        final PrivateKeyData pkd = new PrivateKeyData("val", null, null, null, null);
        final ConfigKeyPair keys = new InlineKeypair("PUB", new KeyDataConfig(pkd, UNLOCKED), keyEncryptor);

        final KeyData marshalledKey = adapter.marshal(keys);

        assertThat(marshalledKey.getPrivateKey()).isNull();
    }

    @Test
    public void unmarshallingDirectKeysGivesCorrectKeypair() {
        final KeyData input = new KeyData();
        input.setPublicKey("public");
        input.setPrivateKey("private");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(DirectKeyPair.class);
    }

    @Test
    public void unmarshallingInlineKeysGivesCorrectKeypair() {
        final KeyData input = new KeyData();
        input.setPublicKey("public");
        input.setConfig(new KeyDataConfig(null, null));

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(InlineKeypair.class);
    }

    @Test
    public void unmarshallingAzureKeysWithNoVersionsGivesCorrectKeyPair() {
        final KeyData input = new KeyData();
        input.setAzureVaultPublicKeyId("pubId");
        input.setAzureVaultPrivateKeyId("privId");
        input.setAzureVaultPublicKeyVersion(null);
        input.setAzureVaultPrivateKeyVersion(null);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(AzureVaultKeyPair.class);
    }

    @Test
    public void unmarshallingAzureKeysWithVersionsGivesCorrectKeyPair() {
        final KeyData input = new KeyData();
        input.setAzureVaultPublicKeyId("pubId");
        input.setAzureVaultPrivateKeyId("privId");
        input.setAzureVaultPublicKeyVersion("pubVer");
        input.setAzureVaultPrivateKeyVersion("privVer");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(AzureVaultKeyPair.class);
    }

    @Test
    public void unmarshallingHashicorpKeysGivesCorrectKeyPair() {
        final KeyData input = new KeyData();

        input.setHashicorpVaultPublicKeyId("pubId");
        input.setHashicorpVaultPrivateKeyId("privId");
        input.setHashicorpVaultSecretEngineName("secretEngine");
        input.setHashicorpVaultSecretName("secretName");
        input.setHashicorpVaultSecretVersion("10");

        HashicorpVaultKeyPair expected = new HashicorpVaultKeyPair("pubId", "privId", "secretEngine", "secretName", 10);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(HashicorpVaultKeyPair.class);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void unmarshallingHashicorpKeysGivesCorrectVersion() {
        final KeyData input = new KeyData();

        input.setHashicorpVaultPublicKeyId("pubId");
        input.setHashicorpVaultPrivateKeyId("privId");
        input.setHashicorpVaultSecretEngineName("secretEngine");
        input.setHashicorpVaultSecretName("secretName");
        input.setHashicorpVaultSecretVersion("10");

        HashicorpVaultKeyPair expected = new HashicorpVaultKeyPair("pubId", "privId", "secretEngine", "secretName", 10);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(HashicorpVaultKeyPair.class);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void unmarshallingHashicorpKeysGivesCorrectVersionNegative() {
        final KeyData input = new KeyData();

        input.setHashicorpVaultPublicKeyId("pubId");
        input.setHashicorpVaultPrivateKeyId("privId");
        input.setHashicorpVaultSecretEngineName("secretEngine");
        input.setHashicorpVaultSecretName("secretName");
        input.setHashicorpVaultSecretVersion("-10");

        HashicorpVaultKeyPair expected = new HashicorpVaultKeyPair("pubId", "privId", "secretEngine", "secretName", -1);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(HashicorpVaultKeyPair.class);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void unmarshallingHashicorpKeysGivesCorrectVersionNonInteger() {
        final KeyData input = new KeyData();

        input.setHashicorpVaultPublicKeyId("pubId");
        input.setHashicorpVaultPrivateKeyId("privId");
        input.setHashicorpVaultSecretEngineName("secretEngine");
        input.setHashicorpVaultSecretName("secretName");
        input.setHashicorpVaultSecretVersion("1.1");

        HashicorpVaultKeyPair expected = new HashicorpVaultKeyPair("pubId", "privId", "secretEngine", "secretName", -1);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(HashicorpVaultKeyPair.class);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void unmarshallingHashicorpKeysGivesCorrectVersionNull() {
        final KeyData input = new KeyData();

        input.setHashicorpVaultPublicKeyId("pubId");
        input.setHashicorpVaultPrivateKeyId("privId");
        input.setHashicorpVaultSecretEngineName("secretEngine");
        input.setHashicorpVaultSecretName("secretName");

        HashicorpVaultKeyPair expected = new HashicorpVaultKeyPair("pubId", "privId", "secretEngine", "secretName", 0);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(HashicorpVaultKeyPair.class);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void unmarshallingAWSKeysGivesCorrectKeyPair() {
        final KeyData input = new KeyData();
        input.setAwsSecretsManagerPublicKeyId("pubId");
        input.setAwsSecretsManagerPrivateKeyId("privId");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(AWSKeyPair.class);
    }

    @Test
    public void unmarshallingPrivateOnlyGivesUnsupportedKeyPair() {
        final KeyData input = new KeyData();
        input.setPrivateKey("private");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingPrivateConfigOnlyGivesUnsupportedKeyPair() {
        final KeyDataConfig keyDataConfig = mock(KeyDataConfig.class);
        final KeyData input = new KeyData();
        input.setConfig(keyDataConfig);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingAzurePublicOnlyGivesUnsupportedKeyPair() {
        final KeyData input = new KeyData();
        input.setAzureVaultPublicKeyId("pubId");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingAzurePrivateOnlyGivesUnsupportedKeyPair() {
        final KeyData input = new KeyData();
        input.setAzureVaultPrivateKeyId("privId");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingHashicorpPublicOnlyGivesUnsupprtedKeyPair() {
        final KeyData input = new KeyData();
        input.setHashicorpVaultPublicKeyId("pubId");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingHashicorpPrivateOnlyGivesUnsupprtedKeyPair() {
        final KeyData input = new KeyData();
        input.setHashicorpVaultPrivateKeyId("privId");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingHashicorpSecretEngineNameOnlyGivesUnsupprtedKeyPair() {
        final KeyData input = new KeyData();
        input.setHashicorpVaultSecretEngineName("secretEngine");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingHashicorpSecretNameOnlyGivesUnsupprtedKeyPair() {
        final KeyData input = new KeyData();
        input.setHashicorpVaultSecretName("secretName");

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingPublicPathOnlyGivesUnsupportedKeyPair() {
        final Path path = mock(Path.class);
        final KeyData input = new KeyData();
        input.setPublicKeyPath(path);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshallingPrivatePathOnlyGivesUnsupportedKeyPair() {
        final Path path = mock(Path.class);
        final KeyData input = new KeyData();
        input.setPrivateKeyPath(path);

        final ConfigKeyPair result = this.adapter.unmarshal(input);
        assertThat(result).isInstanceOf(UnsupportedKeyPair.class);
    }

    @Test
    public void unmarshalWithoutKeyEncryptorReturnNull() {
        KeyEncryptorHolder.INSTANCE.setKeyEncryptor(null);

        KeyData keyData = mock(KeyData.class);
        ConfigKeyPair result = adapter.unmarshal(keyData);
        assertThat(result).isNull();
    }

    @Test
    public void marshalWithoutKeyEncryptorReturnNull() {
        KeyEncryptorHolder.INSTANCE.setKeyEncryptor(null);

        ConfigKeyPair configKeyPair = mock(ConfigKeyPair.class);
        KeyData result = adapter.marshal(configKeyPair);
        assertThat(result).isNull();
    }

    @Test
    public void unmarshalInlineKeyPair() throws Exception {

        KeyData keyData = mock(KeyData.class);
        String tempDir = System.getProperty("java.io.tmpdir");
        Path privateKeyPath = Files.createFile(Paths.get(tempDir, UUID.randomUUID().toString()));
        privateKeyPath.toFile().deleteOnExit();

        Path publicKeyPath = Files.createFile(Paths.get(tempDir, UUID.randomUUID().toString()));
        publicKeyPath.toFile().deleteOnExit();

        when(keyData.getPrivateKeyPath()).thenReturn(privateKeyPath);
        when(keyData.getPublicKeyPath()).thenReturn(publicKeyPath);

        String d =
                "            {\n"
                        + "                \"config\": {\n"
                        + "                    \"data\": {\n"
                        + "                        \"aopts\": {\n"
                        + "                            \"variant\": \"id\",\n"
                        + "                            \"memory\": 1024,\n"
                        + "                            \"iterations\": 1,\n"
                        + "                            \"parallelism\": 1\n"
                        + "                        },\n"
                        + "                        \"snonce\": \"dwixVoY+pOI2FMuu4k0jLqN/naQiTzWe\",\n"
                        + "                        \"asalt\": \"JoPVq9G6NdOb+Ugv+HnUeA==\",\n"
                        + "                        \"sbox\": \"6Jd/MXn29fk6jcrFYGPb75l7sDJae06I3Y1Op+bZSZqlYXsMpa/8lLE29H0sX3yw\"\n"
                        + "                    },\n"
                        + "                    \"type\": \"argon2sbox\"\n"
                        + "                },\n"
                        + "                \"publicKey\": \"UFVCTElDX0tFWQ==\"\n"
                        + "            }";

        InputStream dataIn = new java.io.ByteArrayInputStream(d.getBytes());

        ConfigKeyPair configKeyPair = adapter.unmarshal(keyData);

        assertThat(configKeyPair).isExactlyInstanceOf(FilesystemKeyPair.class);
    }

    @Test
    public void createDefaultInstance() {
        KeyDataAdapter keyDataAdapter = new KeyDataAdapter();
        assertThat(keyDataAdapter).isNotNull();
    }
}
