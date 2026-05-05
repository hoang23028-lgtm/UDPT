declare module 'json-bigint' {
  export interface JsonBigIntOptions {
    strict?: boolean;
    storeAsString?: boolean;
    useNativeBigInt?: boolean;
    alwaysParseAsBig?: boolean;
  }

  export interface JsonBigInt {
    parse(text: string): unknown;
    stringify(value: unknown): string;
  }

  function JSONbig(options?: JsonBigIntOptions): JsonBigInt;
  export default JSONbig;
}
