const { GoogleAuth } = require('google-auth-library');

const auth = new GoogleAuth({
  credentials: JSON.parse(process.env.GOOGLE_SERVICE_ACCOUNT_KEY),
  scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
});

const FCM_URL = `https://fcm.googleapis.com/v1/projects/${process.env.FCM_PROJECT_ID}/messages:send`;

exports.handler = async (event) => {
  const client = await auth.getClient();
  const { token } = await client.getAccessToken();

  const results = await Promise.allSettled(
    event.Records.map(async (record) => {
      const { fcmToken, title, body, data } = JSON.parse(record.body);

      const res = await fetch(FCM_URL, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message: {
            token: fcmToken,
            notification: { title, body },
            data: data ?? {},
          },
        }),
      });

      if (!res.ok) {
        const json = await res.json();
        const errorCode = json.error?.details?.[0]?.errorCode;

        // 토큰 만료 또는 잘못된 토큰 → 재시도 불필요
        if (errorCode === 'UNREGISTERED' || errorCode === 'INVALID_ARGUMENT') {
          console.warn(`FCM 토큰 무효 (스킵): ${fcmToken}`);
          return;
        }

        throw new Error(`FCM 오류: ${json.error?.message}`);
      }
    })
  );

  const batchItemFailures = results
    .map((r, i) =>
      r.status === 'rejected'
        ? { itemIdentifier: event.Records[i].messageId }
        : null
    )
    .filter(Boolean);

  if (batchItemFailures.length > 0) {
    console.error(`실패한 메시지 수: ${batchItemFailures.length}`);
  }

  return { batchItemFailures };
};