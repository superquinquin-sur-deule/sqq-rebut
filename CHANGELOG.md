# Changelog

## [0.16.1](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.16.0...v0.16.1) (2026-06-22)


### Bug Fixes

* do not ignore product weight for pertes ([c78744c](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/c78744c76a9caf7acc206827525ffae93b70498e))

## [0.16.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.15.0...v0.16.0) (2026-06-21)


### Features

* add button to send to rebut on poste interface ([83f9a37](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/83f9a376b64ccf5f80ef7a6ef5232b3104c5f58f))

## [0.15.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.14.0...v0.15.0) (2026-06-19)


### Features

* change scanning behavior for pertes & reassort ([c5578ef](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/c5578efdf05f3937e7314c7ece1e94a6dd12e0cf))

## [0.14.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.13.0...v0.14.0) (2026-06-16)


### Features

* force rebut event when not enough stock for fleg ([ee15afe](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/ee15afeb9020cd5128e929ce742c3c9de7d010da))


### Bug Fixes

* **deps:** update dependency io.quarkiverse.quinoa:quarkus-quinoa to v2.8.3 ([7e35072](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/7e35072355209f55908c182316180c65c17f1c31))

## [0.13.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.12.0...v0.13.0) (2026-06-16)


### Features

* rework releve interface ([48ecb8b](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/48ecb8bee3866f2ec7affb8e273b5d9f3cf51506))


### Bug Fixes

* fix validation message for pertes ([5fef304](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/5fef3044912e580d611b60e4b3726048393e48e5))

## [0.12.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.11.0...v0.12.0) (2026-06-15)


### Features

* add feature to show product stock only ([e621af3](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/e621af3157c74892842a67e86b1618dcc6709d7c))
* allow searching for a product without barcode (ie: salade) ([5a844f0](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/5a844f0a5af1f347a7a58a0b2531e9a1127d8d68))


### Bug Fixes

* **deps:** update quarkus.platform.version to v3.36.2 ([ca7f4dd](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/ca7f4dd4fa032ba8a83197940cd8f6654e000abc))


### Miscellaneous Chores

* **deps:** update actions/setup-node action to v6 ([7945303](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/794530345f65fee92b66e22849acf6f4327eb73d))
* **deps:** update dependency org.wiremock:wiremock-standalone to v3.13.2 ([0216442](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/0216442f37dc25d26331d21027ff4974d01a9d82))
* **deps:** update registry.access.redhat.com/ubi9/openjdk-21-runtime docker tag to v1.24-2.1780581570 ([ff2d94d](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/ff2d94d7764b9bc8360fde9807ea3e61138504e4))
* **deps:** update registry.access.redhat.com/ubi9/ubi-minimal docker tag to v9.8-1780378819 ([4cac7df](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/4cac7df7aa14fbaec51260a9a4dd25279aa90d8d))
* **deps:** update vue monorepo to v3.5.38 ([16eee00](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/16eee004ec70404e2e2a90bd8a8812243c50cb15))

## [0.11.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.10.0...v0.11.0) (2026-06-12)


### Features

* allow searching product by name on scannette interface ([e1959e2](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/e1959e229de9fa28746485183e624007ad1f3485))
* group dlc and pertes separately in scannette releve ([6125912](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/6125912dae19087009d53d2da5f487b93b86ed9e))
* improve scannette hint ([187cac8](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/187cac85aa8fafb3e244aabafebc5246ed943022))

## [0.10.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.9.1...v0.10.0) (2026-06-11)


### Features

* sent pertes directly to rebut ([fb661a7](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/fb661a7365b97fe18d71288f8593127d8e445550))
* support scale barcord without 0 prefix or hash ([284b618](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/284b618427032a3078cb246584e86adabc78e8c3))


### Styles

* prevent rayon column on releve to vary in size ([1ef3392](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/1ef339299077bc95db0c7862c5c7b74394b06d57))

## [0.9.1](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.9.0...v0.9.1) (2026-06-10)


### Bug Fixes

* prevent unwanted scrolling in PWA ([0893614](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/0893614ba8b32751f8925f2b338912845c698f6b))

## [0.9.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.8.0...v0.9.0) (2026-06-10)


### Features

* make app a PWA ([410f770](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/410f7708c7a300417b700258ef394cab2ef57985))


### Documentation

* document app fonctionalities in readme ([7a088e0](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/7a088e0e20796b18d92968dea2c7f6620ea71017))
* reduce screenshots size in readme ([ff8eece](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/ff8eece5af948e81a3ba9f1860f12e2a6de726f0))


### Continuous Integration

* upgrade github actions ([3ff681e](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/3ff681e21c811918d5cd1f999880ad98240d9975))

## [0.8.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.7.0...v0.8.0) (2026-06-10)


### Features

* add healtcheck ([5c3adfe](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/5c3adfe5cfaeb266b7dc58b3fc62b71dc8eae1cd))
* add log on destructive actions ([0700281](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/070028195a18bf91da57a6947085f7b59aeb26a2))
* add releve date in releve page title ([cb877d8](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/cb877d8a30888b9268e493b24996c782360fe41e))
* allow modifying releve from scannettes ([1b315cc](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/1b315ccc3e511df2340679bcfc67a0e7df50676d))


### Miscellaneous Chores

* make release please take commit types into account ([ae5f194](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/ae5f1942a56d9845b7c2914923e770ae737849c2))

## [0.7.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.6.0...v0.7.0) (2026-06-10)


### Features

* cleanup scannettes screen ([c351771](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/c35177118faab48315e3d6d2bc2b5a1208ffb22b))


### Bug Fixes

* update generated api export in frontend ([fb1ddd7](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/fb1ddd74caf59e333860bd2a41fd699cdecbf28b))

## [0.6.0](https://github.com/superquinquin-sur-deule/sqq-rebut/compare/v0.5.0...v0.6.0) (2026-06-10)


### Features

* add bip sound on success/failure scan ([4dc0283](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/4dc0283c76464ffd36a6beb7e6c8ee5e355a3e2e))
* rename app -&gt; Rebut ([aede131](https://github.com/superquinquin-sur-deule/sqq-rebut/commit/aede1310aa01c0daf7d74429f24fd720b34b33de))

## [0.5.0](https://github.com/superquinquin-sur-deule/sqq-dlc/compare/v0.4.0...v0.5.0) (2026-06-10)


### Features

* add home screen to choose between dlc and pertes ([d23e48e](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/d23e48ef107101f1c09f66d69380e12a8a687c5e))
* support price to weight and weight uom ([5cdd124](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/5cdd124f49cf849f83a2dd566c7be962bd83a957))

## [0.4.0](https://github.com/superquinquin-sur-deule/sqq-dlc/compare/v0.3.0...v0.4.0) (2026-06-09)


### Features

* cleanup scanettes screen ([f73c1b2](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/f73c1b20a64403b7b409c43057a3ee5a1b58e682))

## [0.3.0](https://github.com/superquinquin-sur-deule/sqq-dlc/compare/v0.2.1...v0.3.0) (2026-06-09)


### Features

* add releve history ([a0262b4](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/a0262b421fed2fe3dcded09b29105c15d3d02bbe))
* add staging banner when config is true ([2a33690](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/2a33690401b9204ad5bb5f66f88e517ff856623e))
* merge same products and same dlc ([19d3710](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/19d3710d4b1a505ed75041da2b3b271b738e2695))

## [0.2.1](https://github.com/superquinquin-sur-deule/sqq-dlc/compare/v0.2.0...v0.2.1) (2026-06-09)


### Bug Fixes

* null basic auth config is considered empty string ([943a6a6](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/943a6a6a88316aedc708d0422ffee8ad9f7de8d4))

## [0.2.0](https://github.com/superquinquin-sur-deule/sqq-dlc/compare/v0.1.0...v0.2.0) (2026-06-09)


### Features

* avoid showing virtual keyboard when using zebra scanner ([0b820cd](https://github.com/superquinquin-sur-deule/sqq-dlc/commit/0b820cda268c41776e0a74594b6c3f2cfb8d398c))
