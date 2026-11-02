# 온라디오

국내 FM 채널을 인터넷 스트리밍으로 듣는 간결한 Android 앱입니다.

방송 목록과 검색, 기기에 보존되는 즐겨찾기, 스크롤과 독립된 하단 컨트롤러, 별도 재생화면을 제공합니다. 재생화면에 채널/주파수/현재 프로그램/방송시간/프로그램 소개를 표시합니다. KBS·MBC·SBS·CBS 공식 편성 API로 한국시간 당일 편성을 조회하며, 미제공 채널은 공식 편성표로 연결합니다. 화면을 꺼도 Media3 세션 서비스에서 재생하며 미디어 알림·오디오 포커스·이어폰 분리를 지원합니다.

방송 주소는 KBS·MBC·SBS의 공식 스트림 발급 API에서 매번 새로 받습니다. 계정, 자체 중계서버, 유료 API, 녹음 기능은 없습니다. FM 수신칩을 사용하지 않으며 통신 데이터가 필요합니다.

## 빌드

JDK 17 이상, Android SDK 36, Gradle 8.14.5, Android Gradle Plugin 8.9.1.

```sh
./gradlew :app:assembleDebug
```

Termux에서는 `-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`를 지정합니다. DEV applicationId는 `kr.jinsu.radio.dev`입니다. 재배포 시 기존 개발 서명을 유지하고 versionCode를 증가시킵니다. 개인 서명키는 저장소에 포함하지 않습니다.

## 자료

- https://developer.android.com/media/media3/session/background-playback
- KBS: https://cfpwwwapi.kbs.co.kr/ 및 https://static.api.kbs.co.kr/
- MBC: https://mini.imbc.com/ 및 https://sminiplay.imbc.com/
- SBS: https://apis.sbs.co.kr/ 및 https://static.cloud.sbs.co.kr/
- CBS: https://www.cbs.co.kr/ 및 https://m-aac.cbs.co.kr/
- EBS: https://ebsonair.ebs.co.kr/

디자인 참고: Unslop, Damir Sokolovsky (@heydamir), CC BY 4.0, https://github.com/heydamir/unslop . 방송사와 제휴 관계가 없는 개인용 클라이언트입니다.
