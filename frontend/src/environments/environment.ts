export const environment = {
  production: false,
  apiUrl: '/api',
  // Must match AESUtil.SECRET_KEY / INIT_VECTOR on the backend (16 chars = AES-128)
  aesKey: 'LoginApp@AESKey1',
  aesIv: 'LoginApp@AESIv01'
};
