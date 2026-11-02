# 검증 내역

대상: 온라디오 0.1.0-dev1 / kr.jinsu.radio.dev / versionCode 1 / minSdk 26 / targetSdk 36.

- Galaxy-Tab-S11-Ultra의 Termux JDK 21, Gradle 8.14.5, AGP 8.9.1, Android SDK 36, native AAPT2로 빌드했다.
- assembleDebug, lintDebug(오류 0건), 기본 단위 테스트 5건을 통과했다.
- 공식 API를 실제 앱의 RadioData Java 구현으로 호출하여 KBS 5개, MBC 2개, SBS 2개, CBS 2개 채널의 편성 목록과 현재 프로그램을 확인했다. EBS는 공식 편성표 링크를 제공한다.
- KBS·MBC·SBS·CBS·EBS 전체 12개 채널의 HLS 목록과 음성 세그먼트 수신을 확인했다. 이것은 실제 Android 디코더에서의 청취 시험과 구분한다.
- 독립 소스 검토에서 Android API 호환성, 오류 컨트롤 상태, 편성 캐시 만료, 백그라운드 방송정보 갱신, 알림 재진입과 편성표 채널 전환의 상태 일치를 점검하고 발견사항을 수정했다.
- ADB 연결 기기가 없어 설치·실제 화면 렌더링·화면 꺼짐 상태의 장시간 청취·블루투스/전화 통화와의 오디오 전환은 미검증이다. 소스·정적 검사·서버 응답 검증을 실기기 성공으로 표시하지 않는다.

인터넷 방송은 실제 FM 전파 수신과 다르며 데이터 통신을 사용한다. 방송사 공식 API 응답에 따라 편성정보를 갱신한다. 스트리밍 주소는 만료에 대비하여 재생 직전에 새로 발급받는다.
