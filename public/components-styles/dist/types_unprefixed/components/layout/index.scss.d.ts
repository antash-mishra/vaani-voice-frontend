export type Styles = {
  carousel: string;
  'focus-layout': string;
  'focused-participant': string;
  'grid-layout': string;
  'pip-track': string;
};

export type UnprefixedClassNames = keyof Styles;

declare const styles: Styles;

export default styles;